# Expense API

API Spring Boot quản lý khoản chi, lưu dữ liệu bằng SQL Server.
API hỗ trợ tạo, xem, sửa, xóa, lọc, phân trang và thống kê chi tiêu theo tháng.
## Cách chạy

1. Mở thư mục có `pom.xml` bằng IntelliJ IDEA.
2. Chọn JDK cho dự án. Phiên bản Java trong `pom.xml` là 21.
3. Chạy hàm `main` trong `ExpenseApiApplication.java`.
4. Khi cửa sổ Run hiện `Started ExpenseApiApplication`, mở
   `http://localhost:8080/health`. Kết quả mong đợi là `{"status":"ok"}`.

## Xem danh sách khoản chi

Mở `http://localhost:8080/expenses` trong trình duyệt.
Ngay sau khi khởi động, Danh sách trả về các khoản chi đang lưu trong database.

Để lọc theo danh mục, mở `http://localhost:8080/expenses?category=travel`.

## Tạo khoản chi

Chạy các lệnh sau trong PowerShell.

```powershell
$url = 'http://localhost:8080/expenses'
$body = '{"amount":200000,"category":"chatgpt"}'

$result = Invoke-WebRequest -UseBasicParsing -Uri $url -Method Post -ContentType 'application/json' -Body $body
$result.StatusCode
$result.Content
```

Kết quả mong đợi là `201 Created`. Nếu đây là lần tạo thành công đầu tiên
kể từ khi khởi động, khoản mới có `id` là `3`.

Gọi lại `GET /expenses?category=travel` để thấy cả khoản mẫu 20.000 đồng
và khoản vừa tạo 25.000 đồng.

## Kiểm tra dữ liệu sai

Mở `http://localhost:8080/expenses` và ghi lại số khoản chi hiện có.
Sau đó, chạy đoạn sau trong PowerShell để gửi một khoản chi có số tiền âm.

```powershell
$url = 'http://localhost:8080/expenses'
$body = '{"amount":-10000,"category":"travel"}'

try {
    $result = Invoke-WebRequest -UseBasicParsing -Uri $url -Method Post -ContentType 'application/json' -Body $body -ErrorAction Stop
    [int]$result.StatusCode
} catch {
    if ($null -eq $_.Exception.Response) {
        throw
    }
    [int]$_.Exception.Response.StatusCode
}
```

Kết quả mong đợi là `400 Bad Request`. Tải lại `/expenses` và kiểm tra
số khoản chi bằng với số đã ghi trước khi gửi `POST`.

## cach chay spring boot
powerShell
.\mvnw.cmd spring-boot:run

$env:EXPENSE_DB_PASSWORD = [System.Net.NetworkCredential]::new(
'', (Read-Host 'Mat khau SQL Server cua expense_app' -AsSecureString)
).Password

## Chạy kiểm thử

Cần SQL Server đang chạy, database `expense_test_db` có cấu trúc
bảng phù hợp và biến môi trường `EXPENSE_DB_PASSWORD` đã được đặt
trong cửa sổ PowerShell.

Chạy toàn bộ test bằng lệnh sau.

```powershell
.\mvnw.cmd verify
```

Kết quả thành công là `BUILD SUCCESS`.