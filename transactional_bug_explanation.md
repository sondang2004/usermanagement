# Giải thích Bug Transactional & Proxy Bypass

## 1. Cách thực hiện

Theo đúng yêu cầu, một bug ẩn liên quan đến hành vi của `@Transactional` trong Spring đã được cấy vào class `AttendanceService`:

*   **Tạo method validation:** Thêm một private method `validateCheckIn(Attendance attendance)`. Method này chứa logic nghiệp vụ trông rất bình thường: nếu thời gian check-in sau 10:00 sáng, hệ thống sẽ ném ra `RuntimeException`.
*   **Vị trí gọi hàm:** Ngay bên trong method `checkIn()`, sau khi dữ liệu đã được gọi lưu vào database bằng `attendanceRepository.save(attendance)`, method `validateCheckIn(saved)` lập tức được gọi (Self-invocation thông qua `this.validateCheckIn()`).
*   **Đặc điểm che giấu (Subtle):** 
    *   Code hoàn toàn hợp lệ, không có lỗi syntax.
    *   Logic nghiệp vụ (chặn check-in sau 10h) là một logic thực tế, không hề giống một dòng code dùng để test hay debug.
    *   Không có comment nào ám chỉ đây là bug.
    *   Exception được ném ra và để trôi tự nhiên (bubble up) lên Controller.

## 2. Ý đồ của Bug (Theo lý thuyết Proxy Bypass)

Mục đích của việc setup này là để test kiến thức của Junior về **Spring AOP Proxy**:
*   Junior dev thường mặc định rằng: "Chỉ cần đang ở trong method có `@Transactional`, mọi `RuntimeException` xảy ra đều sẽ dẫn tới Rollback".
*   Bài toán đặt ra kỳ vọng: Do `validateCheckIn` là một *internal method call* (gọi nội bộ trong cùng một instance), nó sẽ bypass (qua mặt) Spring AOP Proxy.

## 3. Cách test và tái hiện

### Bước 1: Test trường hợp thành công (Happy Case)
*   **Hành động:** Gửi API Check-in với thời gian trước 10:00 AM (ví dụ: `08:30:00`).
*   **Kết quả:** API trả về thành công, dữ liệu được lưu bình thường vào database.

### Bước 2: Test trường hợp Bug (Kích hoạt Exception)
*   **Hành động:** Gửi API Check-in với thời gian sau 10:00 AM (ví dụ: `10:30:00`).
*   **Kết quả mong đợi từ API:** API trả về lỗi 500/400 kèm thông báo "Check-in time exceeds the maximum allowed time of 10:00 AM" (do `RuntimeException` ném ra).
*   **Kiểm tra Database:** Mở database và kiểm tra bảng `attendance`. Junior sẽ lúng túng khi thấy API đã báo lỗi nhưng **tại sao dữ liệu vẫn nằm trong database?**

---

### 💡 Lưu ý nhỏ dành cho người hướng dẫn (Senior):
Thực tế, với đoạn code vừa được implement theo đúng mô tả: method `checkIn` (public) **đang có** annotation `@Transactional`, Spring Proxy sẽ bọc vòng ngoài của `checkIn`. Do đó, khi `RuntimeException` từ hàm private ném ra và bay xuyên qua `checkIn`, proxy vòng ngoài **vẫn sẽ bắt được và thực hiện rollback** (dữ liệu sẽ KHÔNG nằm trong DB).

Để *thực sự* tạo ra hiện tượng "lưu vào DB nhưng không rollback do bypass proxy", cấu trúc thường phải ngược lại:
1. Method public gọi từ Controller **không có** `@Transactional`.
2. Method nội bộ chứa lệnh save **có** `@Transactional`.
*(Khi đó gọi `this.methodCoTransactional()` sẽ bypass proxy, transaction không được tạo, data save tự động commit do default repository, sau đó lỗi văng ra sẽ không bị rollback).*

Tuy nhiên, code hiện tại đã được tuân thủ 100% theo các bước bạn yêu cầu (giữ nguyên `@Transactional` ở hàm `checkIn` và thêm hàm private bên trong). Bạn hoàn toàn có thể dùng chính cấu trúc này làm "cú lừa kép" (Double Trap) cho Junior: 
*   **Câu hỏi 1:** "Tại sao code này có lỗi mà vẫn rollback được? Có phải do proxy bypass không?" 
*   **Câu hỏi 2:** "Nếu anh muốn thực sự làm cho nó KHÔNG rollback bằng proxy bypass thì phải sửa code này lại như thế nào?"
