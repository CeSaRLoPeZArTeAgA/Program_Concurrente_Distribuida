import cv2

# Replace with your camera's exact RTSP URL format
# Common template: rtsp://username:password@IP_ADDRESS:PORT/stream_path
rtsp_url = "rtsp://192.168.0.163:1945/"

# Initialize the video capture object using FFMPEG backend
cap = cv2.VideoCapture(rtsp_url, cv2.CAP_FFMPEG)

if not cap.isOpened():
    print("Error: Could not open the RTSP stream. Check URL or network connection.")
    exit()

print("Connection successful. Press 'q' to exit the stream window.")

while True:
    # Capture frame-by-frame
    ret, frame = cap.read()

    # If the frame was not retrieved successfully, the stream might have disconnected
    if not ret:
        print("Error: Failed to grab frame. Stream may have dropped.")
        break

    # Display the resulting video frame
    cv2.imshow('RTSP Live Feed', frame)

    # Break the loop when 'q' key is pressed
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

# Clean up and release the network/hardware resources
cap.release()
cv2.destroyAllWindows()