# Start All Backend Services
Write-Host "Starting all backend services..." -ForegroundColor Green

# Start Service Registry first
Write-Host "`n[1/5] Starting Service Registry (Eureka) on port 8761..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'D:\Projects\buy-02\service-registry'; mvn spring-boot:run"
Start-Sleep -Seconds 10

# Start User Service
Write-Host "`n[2/5] Starting User Service on port 8081..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'D:\Projects\buy-02\user-service'; mvn spring-boot:run"
Start-Sleep -Seconds 5

# Start Product Service
Write-Host "`n[3/5] Starting Product Service on port 8082..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'D:\Projects\buy-02\product-service'; mvn spring-boot:run"
Start-Sleep -Seconds 5

# Start Order Service
Write-Host "`n[4/5] Starting Order Service on port 8084..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'D:\Projects\buy-02\order-service'; mvn spring-boot:run"
Start-Sleep -Seconds 5

# Start Media Service
Write-Host "`n[5/5] Starting Media Service on port 8083..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd 'D:\Projects\buy-02\media-service'; mvn spring-boot:run"

Write-Host "`n==============================================================" -ForegroundColor Green
Write-Host "All services are starting!" -ForegroundColor Green
Write-Host "Wait 30-60 seconds for all services to be ready." -ForegroundColor Yellow
Write-Host "==============================================================" -ForegroundColor Green
Write-Host "`nService URLs:"
Write-Host "  - Service Registry: http://localhost:8761" -ForegroundColor White
Write-Host "  - User Service:     http://localhost:8081" -ForegroundColor White
Write-Host "  - Product Service:  http://localhost:8082" -ForegroundColor White
Write-Host "  - Media Service:    http://localhost:8083" -ForegroundColor White
Write-Host "  - Order Service:    http://localhost:8084" -ForegroundColor White
Write-Host "`nFrontend: http://localhost:4200" -ForegroundColor Cyan
