#!/usr/bin/env python3
import gi
import signal
import subprocess

gi.require_version("Gst", "1.0")
gi.require_version("GstRtspServer", "1.0")

from gi.repository import Gst, GstRtspServer, GLib


class RaspiRtspFactory(GstRtspServer.RTSPMediaFactory):
    def __init__(self, width=1280, height=720, fps=15, bitrate=2500):
        super().__init__()
        self.width = width
        self.height = height
        self.fps = fps
        self.bitrate = bitrate

        # Permite que varios clientes usen el mismo stream.
        self.set_shared(True)

    def do_create_element(self, url):
        pipeline = (
            f"libcamerasrc ! "
            f"video/x-raw,width={self.width},height={self.height},framerate={self.fps}/1 ! "
            f"queue ! "
            f"videoconvert ! "
            f"video/x-raw,format=I420 ! "
            f"x264enc tune=zerolatency speed-preset=ultrafast "
            f"bitrate={self.bitrate} "
            f"key-int-max={self.fps * 2} "
            f"byte-stream=true ! "
            f"h264parse config-interval=1 ! "
            f"rtph264pay name=pay0 pt=96 config-interval=1"
        )

        print("[INFO] Pipeline GStreamer:")
        print(pipeline)

        return Gst.parse_launch(pipeline)


def get_interface_ip(interface_name):
    """
    Obtiene la IPv4 de una interfaz específica.
    Para WiFi usamos wlan0.
    """

    try:
        result = subprocess.run(
            ["ip", "-4", "addr", "show", interface_name],
            capture_output=True,
            text=True,
            check=True
        )

        for line in result.stdout.splitlines():
            line = line.strip()

            if line.startswith("inet "):
                # Ejemplo:
                # inet 192.168.0.76/24 brd 192.168.0.255 scope global wlan0
                ip_with_mask = line.split()[1]
                ip = ip_with_mask.split("/")[0]
                return ip

        return None

    except Exception:
        return None


def get_best_ip():
    """
    Prioridad:
    1. wlan0: WiFi
    2. eth0: cable Ethernet
    3. 127.0.0.1 si no encuentra red
    """

    wlan_ip = get_interface_ip("wlan0")
    if wlan_ip:
        return wlan_ip, "wlan0"

    eth_ip = get_interface_ip("eth0")
    if eth_ip:
        return eth_ip, "eth0"

    return "127.0.0.1", "localhost"


def main():
    Gst.init(None)

    width = 1280
    height = 720
    fps = 15
    bitrate = 2500
    port = "8554"
    path = "/raspi"

    server = GstRtspServer.RTSPServer()

    # Escucha en todas las interfaces:
    # wlan0, eth0, localhost, etc.
    server.set_address("0.0.0.0")
    server.set_service(port)

    factory = RaspiRtspFactory(
        width=width,
        height=height,
        fps=fps,
        bitrate=bitrate
    )

    mounts = server.get_mount_points()
    mounts.add_factory(path, factory)

    server.attach(None)

    ip, interface_name = get_best_ip()

    print("[OK] Servidor RTSP iniciado en Raspberry Pi")
    print("[OK] Cámara: CSI / IMX708")
    print(f"[OK] Resolución: {width}x{height}")
    print(f"[OK] FPS: {fps}")
    print(f"[OK] Bitrate: {bitrate} kbps")
    print()
    print("[RED DETECTADA]")
    print(f"Interfaz usada para mostrar la URL: {interface_name}")
    print(f"IP detectada: {ip}")
    print()
    print("[URL PARA TU PC DESKTOP]")
    print(f"rtsp://{ip}:{port}{path}")
    print()
    print("[NOTA]")
    print("El servidor escucha en 0.0.0.0, pero la PC debe conectarse usando la IP real de la red.")
    print("Si trabajas por WiFi, normalmente será la IP de wlan0.")
    print()
    print("[INFO] Presiona CTRL + C para detener.")

    loop = GLib.MainLoop()

    def stop(sig, frame):
        print("\n[INFO] Cerrando servidor RTSP...")
        loop.quit()

    signal.signal(signal.SIGINT, stop)
    signal.signal(signal.SIGTERM, stop)

    loop.run()


if __name__ == "__main__":
    main()