# DeviceDNA Privacy Policy

Effective date: June 29, 2026

Languages:

- [English](#english)
- [Українська](#українська)
- [Русский](#русский)
- [Deutsch](#deutsch)

## English

### Privacy Policy

This Privacy Policy explains how DeviceDNA handles data in the DeviceDNA mobile app.
DeviceDNA is a diagnostics app for viewing hardware, operating system, battery,
storage, network, sensor, camera, app inventory, and device health information.

The developer named for DeviceDNA in the Google Play listing is responsible for
this policy. For privacy requests, use the developer contact email shown in the
Google Play listing for DeviceDNA.

### Summary

DeviceDNA reads device diagnostics to show them in the app. Some features also
sync data to the DeviceDNA backend when you sign in or use sync. DeviceDNA does
not sell personal or sensitive user data.

### Apple Platform Differences

On iOS, DeviceDNA uses Sign in with Apple or Google through Firebase
Authentication and StoreKit for Premium subscriptions. Apple does not expose
installed-app inventory, battery health, charging power, charge cycles, or
numeric component temperatures to third-party apps, so DeviceDNA does not
collect those values on iOS. Storage capacity is read only to display it in the
app; storage values and health scores derived from them are excluded from iOS
backend sync. Widgets use the app's shared container. Background refresh and
Smart Alerts run opportunistically when iOS grants execution time. Google
Mobile Ads is initialized only when the consent SDK reports that ads may be
requested. Account deletion is available in Settings; for accounts created with
Sign in with Apple, DeviceDNA revokes the Apple token before deleting the
Firebase/backend account. References in the Android-specific sections below to
Google Play, Android permissions, Android ID, installed apps, and Battery
Intelligence apply only to Android.

### Data DeviceDNA May Access Or Collect

DeviceDNA may access the following data on your device:

- Device and operating system information, such as manufacturer, model, Android
  version, build information, security patch level, bootloader state, root or
  debug signals, Android ID, supported ABIs, and device health signals.
- Hardware diagnostics, such as CPU information, GPU information, RAM, storage,
  display metrics, thermal zones, battery level, charging state, battery health,
  voltage, current, estimated power, and charge cycle information when available.
- Network and connectivity diagnostics, such as active connection type, local IP
  addresses, gateway, DNS servers, interface name, Wi-Fi metadata, Bluetooth/NFC
  capability, VPN/proxy status, and cellular operator/generation when available.
- Sensor and camera metadata, such as sensor names/vendors and camera
  capabilities. DeviceDNA does not record photos or videos for diagnostics.
- Installed app inventory, including app names, package names, versions,
  install/update timestamps, target SDK versions, and whether an app is a system
  app.
- App settings and local state, such as privacy preferences, display settings,
  premium entitlement cache, widget state, smart alert preferences, and battery
  intelligence history.
- Account information when you sign in with Google through Firebase
  Authentication, such as Firebase UID, email, display name, and profile photo
  URL when provided by Google/Firebase.
- Subscription information when you buy or restore Premium, such as product ID,
  Google Play purchase token or App Store transaction ID, subscription status,
  expiry time, and related transaction/order identifiers.

### Data Sent To The DeviceDNA Backend

When you sign in and use sync, DeviceDNA may transmit a device snapshot to the
DeviceDNA sync backend. This snapshot may include:

- Firebase UID and account profile fields from verified Firebase claims.
- Android ID and device summary fields such as device name, manufacturer, model,
  OS version, and app version.
- A full diagnostics snapshot containing the categories listed above.
- Snapshot hash and sync timestamps.
- Premium subscription status and Google Play or App Store verification data for
  real purchases. A guest App Store purchase is sent after you sign in.

The backend is built with Cloudflare Workers and Cloudflare D1. Data is used to
keep your signed-in DeviceDNA profile, sync device diagnostics, confirm that the
Firebase account still exists, and verify Premium access.

### Data Shared With Third Parties

DeviceDNA uses third-party services for app functionality:

- Google Firebase Authentication for Google sign-in and Firebase ID tokens.
- Google Play Billing for Premium purchases and purchase restoration.
- Google Play Developer API for server-side subscription verification.
- Apple App Store Server API for server-side StoreKit transaction verification.
- Google Mobile Ads / AdMob for ads in the free version.
- Cloudflare Workers for the DeviceDNA backend API.
- Cloudflare D1 for backend database storage.
- api.ipify.org only when you enable the public IP lookup feature.

These providers may process data according to their own privacy terms. DeviceDNA
does not sell your personal or sensitive data.

### Permissions

DeviceDNA requests Android permissions only for related app features:

- Internet and network state are used for sign-in, sync, ads, public IP lookup,
  and subscription verification.
- Billing is used for Google Play Premium subscriptions.
- Notifications are used for Smart Alerts if you enable them.
- Wi-Fi, Bluetooth, phone state, and package visibility permissions are used to
  show diagnostics and hardware/app inventory.
- Camera permission is used for camera metadata and flashlight tests. DeviceDNA
  does not record photos or videos.
- Vibration is used for local haptic feedback.

You can revoke Android permissions in system settings. Some diagnostics may stop
working when a permission is revoked.

### Local Storage And Offline Access

DeviceDNA stores settings and diagnostic history locally on your device. Premium
entitlements are cached locally so Premium features can keep working offline
after the backend has verified a real purchase. The local Premium cache is
encrypted with a key stored in Android Keystore.

Local battery intelligence history and exports remain on your device unless you
share or export them yourself.

### Retention And Deletion

Local app data can be removed by clearing app data or uninstalling DeviceDNA.
Backend account, device, sync, and subscription records are retained while your
DeviceDNA account is active or while needed for service operation, fraud
prevention, subscription verification, or legal compliance.

To request deletion of backend data associated with your DeviceDNA account, use
the developer contact email shown in the Google Play listing. If you delete or
revoke your Firebase/Google account access, the app may no longer be able to sync
or verify the account.

### Security

DeviceDNA uses HTTPS for network communication. Backend user identity is derived
from verified Firebase tokens, not from request bodies. Premium purchase status
is written to the backend only after server-side verification of the Google Play
purchase token. Local Premium cache is encrypted with Android Keystore.

No security measure is perfect, especially on rooted or modified devices, but
DeviceDNA is designed so the backend remains the trusted source for real Premium
status.

### Children

DeviceDNA is a technical diagnostics app and is not directed to children.

### Changes

This policy may be updated when DeviceDNA changes its data handling. Material
changes should be reflected in the app and in the public privacy policy URL used
in Google Play Console.

## Українська

### Політика конфіденційності

Ця Політика конфіденційності пояснює, як DeviceDNA обробляє дані в мобільному
застосунку DeviceDNA. DeviceDNA є діагностичним застосунком для перегляду
інформації про апаратне забезпечення, операційну систему, батарею, сховище,
мережу, сенсори, камери, список встановлених застосунків і стан пристрою.

Розробник, вказаний у сторінці DeviceDNA в Google Play, відповідає за цю
політику. Для запитів щодо конфіденційності використовуйте контактну адресу
розробника, зазначену в Google Play для DeviceDNA.

### Коротко

DeviceDNA зчитує діагностичні дані пристрою, щоб показувати їх у застосунку.
Деякі функції також синхронізують дані з backend DeviceDNA, коли ви входите в
акаунт або використовуєте синхронізацію. DeviceDNA не продає персональні або
чутливі дані користувачів.

### Відмінності На Платформах Apple

На iOS DeviceDNA використовує Sign in with Apple або Google через Firebase
Authentication, а для Premium — StoreKit. Apple не надає стороннім застосункам
список встановлених програм, стан батареї, потужність і цикли заряджання чи
числові температури компонентів, тому DeviceDNA не збирає ці значення на iOS.
Обсяг сховища читається лише для показу в застосунку; значення сховища та
похідний від них health score виключені з iOS backend sync. Віджети використовують
спільний контейнер застосунку, а background refresh і Smart Alerts виконуються
лише тоді, коли iOS надає час. Google Mobile Ads запускається лише після дозволу
consent SDK. Акаунт можна видалити в Settings; для Sign in with Apple спочатку
відкликається Apple token. Згадки нижче про Google Play, Android permissions,
Android ID, installed apps і Battery Intelligence стосуються лише Android.

### Дані, До Яких DeviceDNA Може Отримувати Доступ Або Які Може Збирати

DeviceDNA може отримувати доступ до таких даних на вашому пристрої:

- Дані про пристрій і операційну систему, зокрема виробник, модель, версія
  Android, дані збірки, рівень патча безпеки, стан bootloader, ознаки root або
  debug, Android ID, підтримувані ABI та сигнали стану пристрою.
- Апаратна діагностика, зокрема CPU, GPU, RAM, сховище, параметри дисплея,
  thermal zones, рівень батареї, стан заряджання, стан батареї, напруга, струм,
  оцінена потужність і цикли заряджання, якщо вони доступні.
- Мережева діагностика та підключення, зокрема тип активного з'єднання, локальні
  IP-адреси, gateway, DNS-сервери, назва інтерфейсу, Wi-Fi metadata,
  можливості Bluetooth/NFC, VPN/proxy status і мобільний оператор/покоління
  мережі, якщо доступно.
- Метадані сенсорів і камер, зокрема назви/виробники сенсорів і можливості камер.
  DeviceDNA не записує фото або відео для діагностики.
- Список встановлених застосунків, зокрема назви, package names, версії, час
  встановлення/оновлення, target SDK і ознака системного застосунку.
- Налаштування застосунку та локальний стан, зокрема privacy preferences,
  display settings, кеш Premium entitlement, стан віджетів, налаштування Smart
  Alerts і історія Battery Intelligence.
- Дані акаунта під час входу через Google/Firebase Authentication, зокрема
  Firebase UID, email, display name і URL фото профілю, якщо вони надані
  Google/Firebase.
- Дані підписки під час купівлі або відновлення Premium, зокрема product ID,
  Google Play purchase token або App Store transaction ID, статус підписки, час
  завершення та пов'язані transaction/order identifiers.

### Дані, Які Надсилаються На Backend DeviceDNA

Коли ви входите в акаунт і використовуєте синхронізацію, DeviceDNA може
передавати diagnostics snapshot на backend DeviceDNA. Цей snapshot може
містити:

- Firebase UID і поля профілю акаунта з перевірених Firebase claims.
- Android ID і короткі дані пристрою, зокрема назва пристрою, виробник, модель,
  версія ОС і версія застосунку.
- Повний diagnostics snapshot із категоріями, описаними вище.
- Snapshot hash і timestamps синхронізації.
- Статус Premium subscription і дані Google Play або App Store verification для
  реальних покупок. Гостьова App Store покупка надсилається після входу в акаунт.

Backend побудований на Cloudflare Workers і Cloudflare D1. Дані
використовуються для підтримки signed-in профілю DeviceDNA, синхронізації
діагностики пристрою, перевірки існування Firebase акаунта та підтвердження
Premium доступу.

### Дані, Які Передаються Третім Сторонам

DeviceDNA використовує сторонні сервіси для функціональності застосунку:

- Google Firebase Authentication для Google sign-in і Firebase ID tokens.
- Google Play Billing для покупок Premium і відновлення покупок.
- Google Play Developer API для server-side verification підписки.
- Apple App Store Server API для server-side verification StoreKit transaction.
- Google Mobile Ads / AdMob для реклами у безкоштовній версії.
- Cloudflare Workers для backend API DeviceDNA.
- Cloudflare D1 для backend database storage.
- api.ipify.org лише тоді, коли ви вмикаєте public IP lookup.

Ці провайдери можуть обробляти дані відповідно до власних privacy terms.
DeviceDNA не продає ваші персональні або чутливі дані.

### Дозволи

DeviceDNA запитує Android permissions лише для відповідних функцій:

- Internet і network state використовуються для sign-in, sync, ads, public IP
  lookup і subscription verification.
- Billing використовується для Google Play Premium subscriptions.
- Notifications використовуються для Smart Alerts, якщо ви їх увімкнули.
- Wi-Fi, Bluetooth, phone state і package visibility permissions
  використовуються для показу diagnostics і hardware/app inventory.
- Camera permission використовується для camera metadata і flashlight tests.
  DeviceDNA не записує фото або відео.
- Vibration використовується для локального haptic feedback.

Ви можете відкликати Android permissions у системних налаштуваннях. Деякі
діагностичні функції можуть перестати працювати після відкликання дозволу.

### Локальне Зберігання Та Offline Доступ

DeviceDNA зберігає налаштування та diagnostic history локально на вашому
пристрої. Premium entitlements кешуються локально, щоб Premium функції могли
працювати offline після того, як backend перевірив реальну покупку. Локальний
Premium cache шифрується ключем, що зберігається в Android Keystore.

Локальна історія Battery Intelligence і експортовані звіти залишаються на вашому
пристрої, якщо ви самі ними не поділитеся.

### Зберігання Та Видалення

Локальні дані застосунку можна видалити через очищення app data або видалення
DeviceDNA. Backend records для акаунта, пристрою, sync і subscription
зберігаються, поки ваш DeviceDNA акаунт активний або поки це потрібно для
роботи сервісу, запобігання шахрайству, перевірки підписки чи виконання
законодавчих вимог.

Щоб запросити видалення backend data, пов'язаних із вашим DeviceDNA акаунтом,
використовуйте контактну адресу розробника в Google Play. Якщо ви видалите або
відкличете доступ Firebase/Google акаунта, застосунок може втратити можливість
синхронізації або перевірки акаунта.

### Безпека

DeviceDNA використовує HTTPS для мережевої комунікації. Backend user identity
береться з перевірених Firebase tokens, а не з request body. Premium purchase
status записується на backend лише після server-side verification Google Play
purchase token. Локальний Premium cache шифрується через Android Keystore.

Жоден захід безпеки не є ідеальним, особливо на rooted або modified devices, але
DeviceDNA спроєктовано так, щоб backend залишався trusted source для реального
Premium status.

### Діти

DeviceDNA є технічним діагностичним застосунком і не призначений для дітей.

### Зміни

Ця політика може оновлюватися, коли DeviceDNA змінює спосіб обробки даних.
Суттєві зміни мають бути відображені в застосунку та в публічному privacy policy
URL, який використовується в Google Play Console.

## Русский

### Политика Конфиденциальности

Эта Политика конфиденциальности объясняет, как DeviceDNA обрабатывает данные в
мобильном приложении DeviceDNA. DeviceDNA является диагностическим приложением
для просмотра информации об аппаратном обеспечении, операционной системе,
батарее, хранилище, сети, сенсорах, камерах, списке установленных приложений и
состоянии устройства.

Разработчик, указанный на странице DeviceDNA в Google Play, отвечает за эту
политику. Для запросов, связанных с конфиденциальностью, используйте контактный
email разработчика, указанный в Google Play для DeviceDNA.

### Кратко

DeviceDNA считывает диагностические данные устройства, чтобы показывать их в
приложении. Некоторые функции также синхронизируют данные с backend DeviceDNA,
когда вы входите в аккаунт или используете синхронизацию. DeviceDNA не продаёт
персональные или чувствительные данные пользователей.

### Отличия Платформ Apple

На iOS DeviceDNA использует Sign in with Apple или Google через Firebase
Authentication, а для Premium — StoreKit. Apple не предоставляет сторонним
приложениям список установленных программ, здоровье батареи, мощность и циклы
зарядки или числовые температуры компонентов. Объём хранилища читается только
для показа в приложении; эти значения и рассчитанное по ним состояние исключены
из iOS backend sync. Фоновое обновление и Smart Alerts выполняются только когда
iOS предоставляет время. Google Mobile Ads запускается только после разрешения
consent SDK. Удаление аккаунта доступно в Settings, включая отзыв токена Apple.
Упоминания ниже Google Play, Android permissions, Android ID, installed apps и
Battery Intelligence относятся только к Android.

### Данные, К Которым DeviceDNA Может Получать Доступ Или Которые Может Собирать

DeviceDNA может получать доступ к следующим данным на вашем устройстве:

- Данные об устройстве и операционной системе, включая производителя, модель,
  версию Android, данные сборки, уровень патча безопасности, состояние
  bootloader, признаки root или debug, Android ID, поддерживаемые ABI и сигналы
  состояния устройства.
- Аппаратная диагностика, включая CPU, GPU, RAM, хранилище, параметры дисплея,
  thermal zones, уровень батареи, состояние зарядки, состояние батареи,
  напряжение, ток, оценочную мощность и циклы зарядки, если доступны.
- Сетевая диагностика и подключения, включая тип активного подключения,
  локальные IP-адреса, gateway, DNS-серверы, имя интерфейса, Wi-Fi metadata,
  возможности Bluetooth/NFC, VPN/proxy status и мобильного оператора/поколение
  сети, если доступно.
- Метаданные сенсоров и камер, включая названия/производителей сенсоров и
  возможности камер. DeviceDNA не записывает фото или видео для диагностики.
- Список установленных приложений, включая названия, package names, версии,
  время установки/обновления, target SDK и признак системного приложения.
- Настройки приложения и локальное состояние, включая privacy preferences,
  display settings, кеш Premium entitlement, состояние виджетов, настройки Smart
  Alerts и историю Battery Intelligence.
- Данные аккаунта при входе через Google/Firebase Authentication, включая
  Firebase UID, email, display name и URL фото профиля, если они предоставлены
  Google/Firebase.
- Данные подписки при покупке или восстановлении Premium, включая product ID,
  Google Play purchase token или App Store transaction ID, статус подписки,
  время окончания и связанные transaction/order identifiers.

### Данные, Которые Отправляются На Backend DeviceDNA

Когда вы входите в аккаунт и используете синхронизацию, DeviceDNA может
передавать diagnostics snapshot на backend DeviceDNA. Этот snapshot может
содержать:

- Firebase UID и поля профиля аккаунта из проверенных Firebase claims.
- Android ID и краткие данные устройства, включая название устройства,
  производителя, модель, версию ОС и версию приложения.
- Полный diagnostics snapshot с категориями, описанными выше.
- Snapshot hash и timestamps синхронизации.
- Статус Premium subscription и данные Google Play или App Store verification
  для реальных покупок. Гостевая App Store покупка отправляется после входа.

Backend построен на Cloudflare Workers и Cloudflare D1. Данные используются для
поддержки signed-in профиля DeviceDNA, синхронизации диагностики устройства,
проверки существования Firebase аккаунта и подтверждения Premium доступа.

### Данные, Передаваемые Третьим Сторонам

DeviceDNA использует сторонние сервисы для функций приложения:

- Google Firebase Authentication для Google sign-in и Firebase ID tokens.
- Google Play Billing для покупок Premium и восстановления покупок.
- Google Play Developer API для server-side verification подписки.
- Apple App Store Server API для server-side verification StoreKit transaction.
- Google Mobile Ads / AdMob для рекламы в бесплатной версии.
- Cloudflare Workers для backend API DeviceDNA.
- Cloudflare D1 для backend database storage.
- api.ipify.org только когда вы включаете public IP lookup.

Эти провайдеры могут обрабатывать данные согласно собственным privacy terms.
DeviceDNA не продаёт ваши персональные или чувствительные данные.

### Разрешения

DeviceDNA запрашивает Android permissions только для соответствующих функций:

- Internet и network state используются для sign-in, sync, ads, public IP lookup
  и subscription verification.
- Billing используется для Google Play Premium subscriptions.
- Notifications используются для Smart Alerts, если вы их включили.
- Wi-Fi, Bluetooth, phone state и package visibility permissions используются
  для отображения diagnostics и hardware/app inventory.
- Camera permission используется для camera metadata и flashlight tests.
  DeviceDNA не записывает фото или видео.
- Vibration используется для локального haptic feedback.

Вы можете отозвать Android permissions в системных настройках. Некоторые
диагностические функции могут перестать работать после отзыва разрешения.

### Локальное Хранение И Offline Доступ

DeviceDNA хранит настройки и diagnostic history локально на вашем устройстве.
Premium entitlements кешируются локально, чтобы Premium функции могли работать
offline после того, как backend проверил реальную покупку. Локальный Premium
cache шифруется ключом, который хранится в Android Keystore.

Локальная история Battery Intelligence и экспортированные отчёты остаются на
вашем устройстве, если вы сами ими не поделитесь.

### Хранение И Удаление

Локальные данные приложения можно удалить через очистку app data или удаление
DeviceDNA. Backend records для аккаунта, устройства, sync и subscription
хранятся, пока ваш DeviceDNA аккаунт активен или пока это необходимо для работы
сервиса, предотвращения мошенничества, проверки подписки или выполнения
законодательных требований.

Чтобы запросить удаление backend data, связанных с вашим DeviceDNA аккаунтом,
используйте контактный email разработчика в Google Play. Если вы удалите или
отзовёте доступ Firebase/Google аккаунта, приложение может потерять возможность
синхронизации или проверки аккаунта.

### Безопасность

DeviceDNA использует HTTPS для сетевой коммуникации. Backend user identity
берётся из проверенных Firebase tokens, а не из request body. Premium purchase
status записывается на backend только после server-side verification Google Play
purchase token. Локальный Premium cache шифруется через Android Keystore.

Ни одна мера безопасности не является идеальной, особенно на rooted или modified
devices, но DeviceDNA спроектирован так, чтобы backend оставался trusted source
для реального Premium status.

### Дети

DeviceDNA является техническим диагностическим приложением и не предназначен для
детей.

### Изменения

Эта политика может обновляться, когда DeviceDNA меняет способ обработки данных.
Существенные изменения должны быть отражены в приложении и в публичном privacy
policy URL, который используется в Google Play Console.

## Deutsch

### Datenschutzerklärung

Diese Datenschutzerklärung erklärt, wie DeviceDNA Daten in der mobilen App
DeviceDNA verarbeitet. DeviceDNA ist eine Diagnose-App zum Anzeigen von
Informationen über Hardware, Betriebssystem, Akku, Speicher, Netzwerk, Sensoren,
Kameras, App-Inventar und Gerätezustand.

Der im Google-Play-Eintrag für DeviceDNA genannte Entwickler ist für diese
Erklärung verantwortlich. Für Datenschutzanfragen verwenden Sie die im
Google-Play-Eintrag von DeviceDNA angegebene Entwickler-Kontaktadresse.

### Kurzfassung

DeviceDNA liest Gerätediagnosen, um sie in der App anzuzeigen. Einige Funktionen
synchronisieren Daten auch mit dem DeviceDNA-Backend, wenn Sie sich anmelden
oder Sync verwenden. DeviceDNA verkauft keine personenbezogenen oder sensiblen
Nutzerdaten.

### Unterschiede Auf Apple-Plattformen

Unter iOS verwendet DeviceDNA Sign in with Apple oder Google über Firebase
Authentication und StoreKit für Premium. Apple stellt Drittanbieter-Apps weder
die Liste installierter Apps noch Akkuzustand, Ladeleistung, Ladezyklen oder
numerische Bauteiltemperaturen bereit. Speicherwerte werden nur in der App
angezeigt; sie und daraus berechnete Zustandswerte sind vom iOS-Backend-Sync
ausgeschlossen. Hintergrundaktualisierung und Smart Alerts laufen nur, wenn iOS
Ausführungszeit gewährt. Google Mobile Ads startet nur nach Freigabe durch das
Consent SDK. Das Konto kann in den Einstellungen gelöscht werden, einschließlich
Widerruf des Apple-Tokens. Verweise unten auf Google Play, Android-Berechtigungen,
Android ID, installierte Apps und Battery Intelligence gelten nur für Android.

### Daten, Auf Die DeviceDNA Zugreifen Oder Die DeviceDNA Erfassen Kann

DeviceDNA kann auf folgende Daten auf Ihrem Gerät zugreifen:

- Geräte- und Betriebssysteminformationen, wie Hersteller, Modell,
  Android-Version, Build-Informationen, Sicherheitspatch-Level, Bootloader-Status,
  Root- oder Debug-Signale, Android ID, unterstützte ABIs und Gerätezustandssignale.
- Hardwarediagnosen, wie CPU-Informationen, GPU-Informationen, RAM, Speicher,
  Displaymetriken, thermal zones, Akkustand, Ladestatus, Akkuzustand, Spannung,
  Strom, geschätzte Leistung und Ladezyklen, sofern verfügbar.
- Netzwerk- und Konnektivitätsdiagnosen, wie aktiver Verbindungstyp, lokale
  IP-Adressen, Gateway, DNS-Server, Schnittstellenname, Wi-Fi metadata,
  Bluetooth/NFC-Fähigkeit, VPN/proxy status und Mobilfunkanbieter/-generation,
  sofern verfügbar.
- Sensor- und Kamerametadaten, wie Sensornamen/-hersteller und Kamerafunktionen.
  DeviceDNA zeichnet keine Fotos oder Videos für Diagnosen auf.
- Inventar installierter Apps, einschließlich App-Namen, package names,
  Versionen, Installations-/Aktualisierungszeitpunkte, target SDK und ob eine App
  eine System-App ist.
- App-Einstellungen und lokaler Zustand, wie privacy preferences, display
  settings, Premium-entitlement cache, Widget-Zustand, Smart-Alert-Einstellungen
  und Battery-Intelligence-Verlauf.
- Kontodaten bei Anmeldung über Google/Firebase Authentication, wie Firebase UID,
  E-Mail, Anzeigename und Profilfoto-URL, sofern von Google/Firebase bereitgestellt.
- Abo-Daten beim Kauf oder Wiederherstellen von Premium, wie product ID, Google
  Play purchase token oder App Store transaction ID, Abostatus, Ablaufzeit und
  zugehörige transaction/order identifiers.

### Daten, Die An Das DeviceDNA-Backend Gesendet Werden

Wenn Sie sich anmelden und Sync verwenden, kann DeviceDNA einen diagnostics
snapshot an das DeviceDNA-Sync-Backend übertragen. Dieser Snapshot kann enthalten:

- Firebase UID und Kontoprofilfelder aus geprüften Firebase claims.
- Android ID und Geräteübersicht, wie Gerätename, Hersteller, Modell, OS-Version
  und App-Version.
- Einen vollständigen diagnostics snapshot mit den oben genannten Kategorien.
- Snapshot hash und Sync-Zeitstempel.
- Premium subscription status und Google-Play- oder App-Store-Verifizierungsdaten
  für echte Käufe. Ein Gastkauf wird nach der Anmeldung übertragen.

Das Backend basiert auf Cloudflare Workers und Cloudflare D1. Die Daten werden
verwendet, um Ihr angemeldetes DeviceDNA-Profil zu verwalten, Gerätediagnosen zu
synchronisieren, das Bestehen des Firebase-Kontos zu bestätigen und Premium-Zugang
zu verifizieren.

### Daten, Die Mit Dritten Geteilt Werden

DeviceDNA verwendet Drittanbieter-Dienste für App-Funktionen:

- Google Firebase Authentication für Google sign-in und Firebase ID tokens.
- Google Play Billing für Premium-Käufe und Wiederherstellung von Käufen.
- Google Play Developer API für server-side verification des Abos.
- Apple App Store Server API für server-side verification der StoreKit transaction.
- Google Mobile Ads / AdMob für Werbung in der kostenlosen Version.
- Cloudflare Workers für die DeviceDNA backend API.
- Cloudflare D1 für backend database storage.
- api.ipify.org nur, wenn Sie public IP lookup aktivieren.

Diese Anbieter können Daten gemäß ihren eigenen privacy terms verarbeiten.
DeviceDNA verkauft Ihre personenbezogenen oder sensiblen Daten nicht.

### Berechtigungen

DeviceDNA fordert Android permissions nur für zugehörige App-Funktionen an:

- Internet und network state werden für sign-in, sync, ads, public IP lookup und
  subscription verification verwendet.
- Billing wird für Google Play Premium subscriptions verwendet.
- Notifications werden für Smart Alerts verwendet, wenn Sie diese aktivieren.
- Wi-Fi, Bluetooth, phone state und package visibility permissions werden
  verwendet, um diagnostics und hardware/app inventory anzuzeigen.
- Camera permission wird für camera metadata und flashlight tests verwendet.
  DeviceDNA zeichnet keine Fotos oder Videos auf.
- Vibration wird für lokales haptic feedback verwendet.

Sie können Android permissions in den Systemeinstellungen widerrufen. Einige
Diagnosefunktionen funktionieren möglicherweise nicht mehr, wenn eine Berechtigung
widerrufen wird.

### Lokale Speicherung Und Offline-Zugriff

DeviceDNA speichert Einstellungen und diagnostic history lokal auf Ihrem Gerät.
Premium entitlements werden lokal zwischengespeichert, damit Premium-Funktionen
offline weiter funktionieren können, nachdem das Backend einen echten Kauf
verifiziert hat. Der lokale Premium cache wird mit einem Schlüssel verschlüsselt,
der im Android Keystore gespeichert ist.

Der lokale Battery-Intelligence-Verlauf und Exporte bleiben auf Ihrem Gerät, es
sei denn, Sie teilen oder exportieren sie selbst.

### Aufbewahrung Und Löschung

Lokale App-Daten können durch Löschen der App-Daten oder Deinstallieren von
DeviceDNA entfernt werden. Backend records für Konto, Gerät, Sync und subscription
werden aufbewahrt, solange Ihr DeviceDNA-Konto aktiv ist oder solange dies für
Servicebetrieb, Betrugsprävention, Abo-Verifizierung oder gesetzliche Pflichten
erforderlich ist.

Um die Löschung von backend data zu Ihrem DeviceDNA-Konto zu beantragen,
verwenden Sie die Entwickler-Kontaktadresse im Google-Play-Eintrag. Wenn Sie den
Firebase/Google-Kontozugriff löschen oder widerrufen, kann die App möglicherweise
nicht mehr synchronisieren oder das Konto verifizieren.

### Sicherheit

DeviceDNA verwendet HTTPS für Netzwerkkommunikation. Die backend user identity
wird aus verifizierten Firebase tokens abgeleitet, nicht aus request bodies.
Premium purchase status wird nur nach server-side verification des Google Play
purchase token in das Backend geschrieben. Der lokale Premium cache wird über
Android Keystore verschlüsselt.

Keine Sicherheitsmaßnahme ist perfekt, insbesondere auf rooted oder modified
devices. DeviceDNA ist jedoch so konzipiert, dass das Backend die trusted source
für echten Premium status bleibt.

### Kinder

DeviceDNA ist eine technische Diagnose-App und richtet sich nicht an Kinder.

### Änderungen

Diese Erklärung kann aktualisiert werden, wenn DeviceDNA seine Datenverarbeitung
ändert. Wesentliche Änderungen sollten in der App und in der öffentlichen privacy
policy URL, die in der Google Play Console verwendet wird, widergespiegelt werden.
