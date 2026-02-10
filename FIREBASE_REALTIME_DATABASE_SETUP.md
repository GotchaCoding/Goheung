# Firebase Realtime Database 설정 가이드

## 1. Firebase Console 설정

### Realtime Database 활성화
1. [Firebase Console](https://console.firebase.google.com/) 접속
2. 프로젝트 선택 (Goheung)
3. 좌측 메뉴에서 **Build > Realtime Database** 선택
4. **Create Database** 버튼 클릭
5. 데이터베이스 위치 선택 (asia-northeast3 권장 - 서울)
6. 보안 규칙: **Start in locked mode** 선택 후 Enable

### 보안 규칙 설정

**Rules** 탭에서 다음 규칙 적용:

```json
{
  "rules": {
    "presence": {
      "$uid": {
        ".read": "auth != null",
        ".write": "auth.uid == $uid",
        "attendance": {
          ".write": "auth != null"
        }
      }
    }
  }
}
```

**규칙 설명:**
- **read**: 모든 인증된 사용자가 다른 사용자의 Presence 상태를 읽을 수 있음
- **write**: 자신의 Presence만 업데이트 가능
- **attendance.write**: 인증된 사용자는 다른 사용자의 근무 상태도 변경 가능 (관리자 기능)

### 인덱싱 설정 (선택사항)
성능 향상을 위해 인덱스 추가:

```json
{
  "rules": {
    "presence": {
      ".indexOn": ["online", "lastActive"]
    }
  }
}
```

## 2. 데이터 구조

### Presence 노드 구조
```
presence/
  {uid}/
    uid: "사용자ID"
    online: true/false
    lastActive: 1234567890  // timestamp
    inChat: true/false
    chatRoomId: "채팅방ID" or null
    attendance/
      uid: "사용자ID"
      status: "WORKING" | "REMOTE" | "ON_LEAVE" | "HALF_DAY"
      updatedAt: 1234567890  // timestamp
```

## 3. 테스트

### 앱 실행 후 확인사항

1. **Firebase Console > Realtime Database > Data 탭**에서 실시간 데이터 확인
2. 앱 로그인 시 `presence/{uid}/online = true` 자동 생성 확인
3. 앱 종료 시 `online = false` 자동 변경 확인
4. Spinner에서 근무 상태 변경 시 `attendance/status` 업데이트 확인

### 예상 데이터 (예시)
```json
{
  "presence": {
    "user123abc": {
      "uid": "user123abc",
      "online": true,
      "lastActive": 1707552000000,
      "inChat": false,
      "chatRoomId": null,
      "attendance": {
        "uid": "user123abc",
        "status": "WORKING",
        "updatedAt": 1707552000000
      }
    }
  }
}
```

## 4. 문제 해결

### Presence가 업데이트 되지 않는 경우
- Firebase Console에서 보안 규칙 확인
- 앱 로그에서 `PresenceRepository` TAG 확인
- Firebase SDK 인증 상태 확인: `FirebaseAuth.getInstance().currentUser`

### Offline 상태가 자동으로 변경되지 않는 경우
- `.info/connected` 리스너가 정상 동작하는지 로그 확인
- `onDisconnect()` 핸들러가 설정되었는지 확인
- 네트워크 연결 상태 확인

### Attendance 변경이 안 되는 경우
- 보안 규칙에서 `attendance` 경로 write 권한 확인
- 로그에서 `AttendanceRepository` TAG 확인
- Firebase Console > Data에서 경로 구조 확인

## 5. 모니터링

### 실시간 모니터링
Firebase Console > Realtime Database > Usage 탭:
- 동시 연결 수
- 데이터 전송량
- 읽기/쓰기 작업 수

### 무료 할당량 (Spark Plan)
- 동시 연결: 100개
- 저장 용량: 1GB
- 다운로드: 10GB/월
- 업로드: 무제한

### 프로덕션 추천사항
- Blaze Plan (종량제) 고려
- 보안 규칙 강화 (부서별 권한 등)
- 백업 활성화
