import socket
import concurrent.futures
import subprocess
import sys

IP = "192.168.0.104"
ADB = r"C:\Users\anupam kumar\AppData\Local\Android\Sdk\platform-tools\adb.exe"

def check_port(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.settimeout(0.06)
        if s.connect_ex((IP, port)) == 0:
            return port
    return None

def main():
    print(f"Scanning {IP} across ports 30000-50000 for wireless ADB...")
    open_ports = []
    with concurrent.futures.ThreadPoolExecutor(max_workers=250) as executor:
        futures = {executor.submit(check_port, p): p for p in range(30000, 50001)}
        for future in concurrent.futures.as_completed(futures):
            res = future.result()
            if res:
                print(f"--> Found open port: {res}")
                open_ports.append(res)
    
    if not open_ports:
        print("No open high ports found on 192.168.0.104.")
        return
        
    for port in open_ports:
        target = f"{IP}:{port}"
        print(f"Trying adb connect {target}...")
        subprocess.run([ADB, "connect", target])
        
    subprocess.run([ADB, "devices"])

if __name__ == "__main__":
    main()
