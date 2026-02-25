import * as functions from 'firebase-functions';
import * as admin from 'firebase-admin';

// Initialize Firebase Admin if not already initialized
if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.database();
const firestore = admin.firestore();

// 알림 쿨다운 시간 (밀리초)
const NOTIFICATION_COOLDOWN_MS = 60000; // 1분

// 버스 도착 알림 거리 (미터)
const BUS_ARRIVAL_DISTANCE_M = 500;

// 최근 알림 전송 시간 저장 (메모리 캐시)
const lastNotificationTime: Map<string, number> = new Map();

/**
 * 위치 변경 시 버스 도착 알림 전송
 * /locations/{uid} 경로의 변경을 감지
 */
export const onLocationUpdate = functions.database
  .ref('/locations/{uid}')
  .onUpdate(async (change, context) => {
    const uid = context.params.uid;
    const newData = change.after.val();

    // DRIVER 역할이 아니면 무시
    if (newData.role !== 'DRIVER') {
      return null;
    }

    const driverLat = newData.lat;
    const driverLng = newData.lng;
    const driverName = newData.displayName || '버스';

    console.log(`Driver ${driverName} (${uid}) location updated: ${driverLat}, ${driverLng}`);

    // 모든 위치 데이터 조회
    const locationsSnapshot = await db.ref('/locations').once('value');
    const locations = locationsSnapshot.val();

    if (!locations) {
      return null;
    }

    // PASSENGER 역할 필터 및 500m 이내 확인
    const notifications: Promise<void>[] = [];

    for (const [passengerUid, location] of Object.entries(locations)) {
      const passengerLocation = location as {
        role: string;
        lat: number;
        lng: number;
        displayName: string;
      };

      // 본인이거나 PASSENGER가 아니면 스킵
      if (passengerUid === uid || passengerLocation.role !== 'PASSENGER') {
        continue;
      }

      // 거리 계산
      const distance = calculateDistance(
        driverLat,
        driverLng,
        passengerLocation.lat,
        passengerLocation.lng
      );

      // 500m 이내인 경우 알림 전송
      if (distance <= BUS_ARRIVAL_DISTANCE_M) {
        const notificationKey = `${uid}_${passengerUid}`;

        // 쿨다운 확인
        const lastTime = lastNotificationTime.get(notificationKey) || 0;
        const now = Date.now();

        if (now - lastTime < NOTIFICATION_COOLDOWN_MS) {
          console.log(`Skipping notification for ${passengerUid} (cooldown)`);
          continue;
        }

        // 쿨다운 업데이트
        lastNotificationTime.set(notificationKey, now);

        // 알림 전송
        notifications.push(
          sendBusArrivalNotification(
            passengerUid,
            driverName,
            Math.round(distance)
          )
        );
      }
    }

    await Promise.all(notifications);
    return null;
  });

/**
 * 버스 도착 FCM 알림 전송
 */
async function sendBusArrivalNotification(
  passengerUid: string,
  driverName: string,
  distanceMeters: number
): Promise<void> {
  try {
    // Firestore에서 사용자의 FCM 토큰 조회
    const userDoc = await firestore.collection('users').doc(passengerUid).get();
    const userData = userDoc.data();

    if (!userData?.fcmToken) {
      console.log(`No FCM token for user ${passengerUid}`);
      return;
    }

    const fcmToken = userData.fcmToken;
    const distanceText = distanceMeters >= 1000
      ? `${(distanceMeters / 1000).toFixed(1)}km`
      : `${distanceMeters}m`;

    const message: admin.messaging.Message = {
      token: fcmToken,
      data: {
        type: 'BUS_ARRIVAL',
        driverName: driverName,
        distance: distanceText,
      },
      android: {
        priority: 'high',
        notification: {
          channelId: 'goheung_bus_arrival',
        },
      },
    };

    await admin.messaging().send(message);
    console.log(`Bus arrival notification sent to ${passengerUid}`);
  } catch (error) {
    console.error(`Failed to send notification to ${passengerUid}:`, error);
  }
}

/**
 * Haversine 공식을 사용한 두 좌표 간 거리 계산 (미터)
 */
function calculateDistance(
  lat1: number,
  lng1: number,
  lat2: number,
  lng2: number
): number {
  const R = 6371000; // 지구 반경 (미터)
  const dLat = toRadians(lat2 - lat1);
  const dLng = toRadians(lng2 - lng1);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRadians(lat1)) *
      Math.cos(toRadians(lat2)) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);

  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return R * c;
}

function toRadians(degrees: number): number {
  return degrees * (Math.PI / 180);
}
