$ip = "192.168.0.104"
Write-Host "Scanning $ip for Wireless Debugging port..."

$ports = 33000..47000
$found = $null

$threads = [System.Collections.Concurrent.ConcurrentBag[int]]::new()

$ports | ForEach-Object -Parallel {
    $p = $_
    $sock = New-Object System.Net.Sockets.Socket([System.Net.Sockets.AddressFamily]::InterNetwork, [System.Net.Sockets.SocketType]::Stream, [System.Net.Sockets.ProtocolType]::Tcp)
    $async = $sock.BeginConnect("192.168.0.104", $p, $null, $null)
    $success = $async.AsyncWaitHandle.WaitOne(45, $false)
    if ($success) {
        try {
            $sock.EndConnect($async)
            Write-Host "FOUND OPEN PORT: $p" -ForegroundColor Green
            $threads.Add($p)
        } catch {}
    }
    $sock.Close()
} -ThrottleLimit 200

$openPorts = $threads.ToArray()
Write-Host "Open ports: $($openPorts -join ', ')"
foreach ($port in $openPorts) {
    Write-Host "Testing adb connect $ip`:$port ..."
    & "C:\Users\anupam kumar\AppData\Local\Android\Sdk\platform-tools\adb.exe" connect "$ip`:$port"
}
& "C:\Users\anupam kumar\AppData\Local\Android\Sdk\platform-tools\adb.exe" devices
