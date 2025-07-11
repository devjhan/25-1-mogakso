import sys
from . import ChatClient

def handle_incoming_message(client : ChatClient, msg_type : int, payload : bytes):
    """서버로부터 메시지를 수신했을 때 실행될 콜백 함수"""
    if msg_type == 1:
        try :
            message = payload.decode('utf-8')
            print(f"\r[수신] {message}\n> ", end="", flush=True)
        except UnicodeDecodeError:
            print(f"\r[수신] (디코딩 불가 메시지, 길이: {len(payload)})\n> ", end="", flush=True)
    else:
        print(f"\r[수신] (타입: {msg_type}, 길이: {len(payload)})\n> ", end="", flush=True)

def handle_error(client : ChatClient, error_code : int, message : str):
    """에러가 발생했을 때 실행될 콜백 함수"""
    print(f"\r[오류 발생] 코드: {error_code}, 메시지: {message}\n> ", end="", flush=True)

if __name__ == '__main__':
    host = sys.argv[1] if len(sys.argv) > 1 else '127.0.0.1'
    port = int(sys.argv[2]) if len(sys.argv) > 2 else 8080
    username = sys.argv[3] if len(sys.argv) > 3 else "user"

    client = ChatClient(username)

    client.on_message = handle_incoming_message
    client.on_error = handle_error

    if not client.connect(host, port):
        sys.exit(1)

    client.start_in_background()

    print("--------------------------------------------------")
    print("채팅 클라이언트가 시작되었습니다.")
    print("메시지를 입력하거나 아래 명령어를 사용하세요:")
    print(" - /file [파일 경로]  : 파일 전송")
    print(" - exit 또는 Ctrl+C  : 프로그램 종료")
    print("--------------------------------------------------")

    try:
        while True:
            user_input = input("> ")

            if user_input.lower() == 'exit':
                break

            if user_input.startswith('/file '):
                # 파일 전송 명령어 처리
                filepath = user_input.split(' ', 1)[1]
                if client.send_file(filepath):
                    print(f"'{filepath}' 파일 전송을 요청했습니다.")
                else:
                    print("파일 전송 요청에 실패했습니다.")
            elif user_input:
                # 일반 텍스트 메시지 전송
                client.send_message(user_input)
    except (KeyboardInterrupt, EOFError):
        print("\n프로그램을 종료합니다.")
    finally:
        client.disconnect()
        print("연결이 안전하게 종료되었습니다.")