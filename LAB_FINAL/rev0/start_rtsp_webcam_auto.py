import argparse
import os
import re
import shutil
import socket
import subprocess
import sys
import time
from pathlib import Path


def find_vlc():
    """
    Busca VLC en rutas típicas de Windows o en el PATH.
    """
    candidates = [
        r"C:\Program Files\VideoLAN\VLC\vlc.exe",
        r"C:\Program Files (x86)\VideoLAN\VLC\vlc.exe",
    ]

    for path in candidates:
        if os.path.exists(path):
            return path

    return shutil.which("vlc")


def find_ffmpeg():
    """
    Busca ffmpeg en el PATH.
    """
    return shutil.which("ffmpeg")


def get_local_ip():
    """
    Detecta automáticamente la IP activa de la PC.
    Sirve aunque cambies de red WiFi.
    """
    targets = [
        ("8.8.8.8", 80),
        ("1.1.1.1", 80),
        ("192.168.0.1", 80),
        ("192.168.1.1", 80),
    ]

    for target_ip, target_port in targets:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        try:
            sock.connect((target_ip, target_port))
            ip = sock.getsockname()[0]

            if not ip.startswith("127."):
                return ip

        except Exception:
            pass
        finally:
            sock.close()

    try:
        ip = socket.gethostbyname(socket.gethostname())
        return ip
    except Exception:
        return "127.0.0.1"


def list_directshow_cameras():
    """
    Lista cámaras DirectShow usando ffmpeg.
    Devuelve una lista con los nombres de cámaras detectadas.
    """
    ffmpeg = find_ffmpeg()

    if not ffmpeg:
        print("[ERROR] No se encontró ffmpeg en el PATH.")
        print("Instala FFmpeg o agrégalo al PATH.")
        return []

    cmd = [
        ffmpeg,
        "-list_devices",
        "true",
        "-f",
        "dshow",
        "-i",
        "dummy"
    ]

    result = subprocess.run(
        cmd,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
        encoding="utf-8",
        errors="ignore"
    )

    output = result.stderr + result.stdout

    cameras = []

    for line in output.splitlines():
        match = re.search(r'"(.+?)"\s+\(video\)', line)
        if match:
            cameras.append(match.group(1))

    return cameras


def kill_vlc():
    """
    Cierra VLC si fue iniciado por el usuario actual.
    No requiere administrador si VLC pertenece al mismo usuario.
    """
    subprocess.run(
        ["taskkill", "/IM", "vlc.exe", "/F"],
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL
    )


def is_port_listening(port):
    """
    Verifica si el puerto RTSP quedó activo localmente.
    """
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(1)

    try:
        return sock.connect_ex(("127.0.0.1", port)) == 0
    finally:
        sock.close()


def save_run_files(rtsp_url, camera_name, ip, port):
    """
    Guarda información útil para usar desde otra PC.
    """
    Path("rtsp_url.txt").write_text(rtsp_url, encoding="utf-8")

    info = f"""Servidor RTSP Webcam

Camara: {camera_name}
IP: {ip}
Puerto: {port}
URL RTSP: {rtsp_url}

Prueba desde otra PC:
ffplay -rtsp_transport udp "{rtsp_url}"

Detener:
python start_rtsp_webcam_auto.py --stop
"""

    Path("rtsp_info.txt").write_text(info, encoding="utf-8")


def start_rtsp_server(args):
    vlc_path = find_vlc()

    if not vlc_path:
        print("[ERROR] No se encontró VLC.")
        print("Instala VLC o verifica que exista en:")
        print(r"C:\Program Files\VideoLAN\VLC\vlc.exe")
        sys.exit(1)

    cameras = list_directshow_cameras()

    if not cameras:
        print("[ERROR] No se detectó ninguna cámara.")
        print("Verifica que la webcam esté conectada y no esté ocupada por otra aplicación.")
        sys.exit(1)

    if args.camera:
        camera_name = args.camera

        if camera_name not in cameras:
            print("[ADVERTENCIA] La cámara indicada no aparece en la lista detectada.")
            print("[INFO] Cámaras detectadas:")
            for cam in cameras:
                print("  -", cam)
            print()
            print("[INFO] Se intentará usar igualmente:", camera_name)
    else:
        camera_name = cameras[0]

    ip = args.ip if args.ip else get_local_ip()
    path = args.path.strip("/")

    # URL que se mostrará al usuario para conectarse desde otra PC.
    public_rtsp_url = f"rtsp://{ip}:{args.port}/{path}"

    # URL interna que necesita VLC para publicar el servidor RTSP.
    # Esta forma fue la que funcionó en tu prueba manual.
    vlc_sdp_url = f"rtsp://:{args.port}/{path}"

    if args.kill_previous:
        print("[INFO] Cerrando VLC anterior si existe...")
        kill_vlc()
        time.sleep(1)

    sout = (
        f"#transcode{{"
        f"vcodec=h264,"
        f"vb={args.bitrate},"
        f"fps={args.fps},"
        f"width={args.width},"
        f"height={args.height},"
        f"acodec=none"
        f"}}:"
        f"rtp{{sdp={vlc_sdp_url}}}"
    )

    cmd = [
        vlc_path,
        "dshow://",
        f':dshow-vdev="{camera_name}"',
        ":dshow-adev=none",
        f":dshow-size={args.width}x{args.height}",
        f":dshow-fps={args.fps}",
        f":dshow-chroma={args.chroma}",
        "--qt-start-minimized",
        "--no-video-title-show",
        f"--sout={sout}",
        "--no-sout-all",
        "--sout-keep"
    ]

    print()
    print("==============================================")
    print(" SERVIDOR RTSP DE WEBCAM")
    print("==============================================")
    print("[INFO] VLC:", vlc_path)
    print("[INFO] Cámara:", camera_name)
    print("[INFO] IP detectada:", ip)
    print("[INFO] Puerto:", args.port)
    print("[INFO] Resolución:", f"{args.width}x{args.height}")
    print("[INFO] FPS:", args.fps)
    print("[INFO] Bitrate:", args.bitrate, "kbps")
    print("[INFO] URL RTSP:", public_rtsp_url)
    print("==============================================")
    print()

    try:
        subprocess.Popen(
            cmd,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            creationflags=subprocess.CREATE_NEW_PROCESS_GROUP
        )
    except Exception as e:
        print("[ERROR] No se pudo iniciar VLC.")
        print(e)
        sys.exit(1)

    time.sleep(4)

    if is_port_listening(args.port):
        save_run_files(public_rtsp_url, camera_name, ip, args.port)

        print("[OK] VLC está transmitiendo en segundo plano.")
        print("[OK] URL para usar desde otra PC:")
        print(public_rtsp_url)
        print()
        print("[OK] Se guardó también en:")
        print(os.path.abspath("rtsp_url.txt"))
        print(os.path.abspath("rtsp_info.txt"))
        print()
        print("[PRUEBA DESDE LA PC RECEPTORA]")
        print(f'ffplay -rtsp_transport udp "{public_rtsp_url}"')
        print()
        print("[PARA DETENER]")
        print("python start_rtsp_webcam_auto.py --stop")
    else:
        print("[ERROR] VLC inició, pero el puerto RTSP no quedó activo.")
        print()
        print("Prueba con menor resolución:")
        print("python start_rtsp_webcam_auto.py --width 640 --height 480 --bitrate 1200")
        print()
        print("También revisa que la cámara no esté ocupada por otra aplicación.")


def stop_rtsp_server():
    kill_vlc()
    print("[OK] VLC detenido.")


def show_cameras():
    cameras = list_directshow_cameras()

    if not cameras:
        print("[ERROR] No se encontraron cámaras.")
        return

    print("[INFO] Cámaras detectadas:")
    for idx, camera in enumerate(cameras, start=1):
        print(f"{idx}. {camera}")


def main():
    parser = argparse.ArgumentParser(
        description="Inicia una webcam como servidor RTSP usando VLC en Windows."
    )

    parser.add_argument(
        "--camera",
        default=None,
        help="Nombre exacto de la cámara. Si no se indica, usa la primera detectada."
    )

    parser.add_argument(
        "--ip",
        default=None,
        help="IP manual. Si no se indica, se detecta automáticamente."
    )

    parser.add_argument(
        "--port",
        type=int,
        default=8555,
        help="Puerto RTSP."
    )

    parser.add_argument(
        "--path",
        default="webcam",
        help="Ruta RTSP. Ejemplo: webcam"
    )

    parser.add_argument(
        "--width",
        type=int,
        default=1280,
        help="Ancho del video."
    )

    parser.add_argument(
        "--height",
        type=int,
        default=720,
        help="Alto del video."
    )

    parser.add_argument(
        "--fps",
        type=int,
        default=30,
        help="Frames por segundo."
    )

    parser.add_argument(
        "--bitrate",
        type=int,
        default=2500,
        help="Bitrate H264 en kbps."
    )

    parser.add_argument(
        "--chroma",
        default="MJPG",
        help="Formato de entrada de cámara. Normalmente MJPG."
    )

    parser.add_argument(
        "--no-kill",
        action="store_true",
        help="No cerrar VLC anterior antes de iniciar."
    )

    parser.add_argument(
        "--stop",
        action="store_true",
        help="Detener VLC y salir."
    )

    parser.add_argument(
        "--list-cameras",
        action="store_true",
        help="Mostrar cámaras disponibles."
    )

    args = parser.parse_args()
    args.kill_previous = not args.no_kill

    if args.stop:
        stop_rtsp_server()
        return

    if args.list_cameras:
        show_cameras()
        return

    start_rtsp_server(args)


if __name__ == "__main__":
    main()