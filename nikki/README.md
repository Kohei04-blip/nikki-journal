# Nikki — Spring Boot 学習用ジャーナルアプリ

## 技術スタック

| 技術 | 役割 |
|------|------|
| Spring Boot 3.2 | アプリ基盤 |
| Spring MVC | URLルーティング・Controller |
| Spring Data JPA | DB操作 |
| Thymeleaf | HTMLテンプレート |
| PostgreSQL | 本番DB（Renderで使用） |
| H2 Database | ローカル開発用DB（設定不要） |
| Java 17 | 言語 |

---

## ファイル構成

```
nikki/
├── pom.xml                                   ← 依存ライブラリの定義
└── src/main/
    ├── java/com/example/nikki/
    │   ├── NikkiApplication.java             ← 起動クラス
    │   ├── DataInitializer.java              ← デモデータ投入（localのみ）
    │   ├── entity/Entry.java                 ← DBテーブルに対応
    │   ├── repository/EntryRepository.java   ← DB操作
    │   ├── service/EntryService.java         ← ビジネスロジック
    │   └── controller/EntryController.java   ← URLとHTMLをつなぐ
    └── resources/
        ├── application.properties            ← 共通 + 本番設定
        ├── application-local.properties      ← ローカル開発用（H2）
        ├── application-prod.properties       ← 本番用（Render）
        └── templates/
            ├── today.html                    ← 今日の記録
            ├── history.html                  ← 一覧・検索
            ├── edit.html                     ← 編集
            ├── detail.html                   ← 詳細
            └── reflect.html                  ← 振り返り
```

---

## ローカル起動（H2 インメモリDB）

```bash
# local プロファイルで起動（H2を使う）
mvn spring-boot:run -Dspring-boot.run.profiles=local

# ブラウザで開く
# http://localhost:8080

# H2コンソール（DBの中身を確認）
# http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:nikkidb  Username: sa  Password: (空)
```

---

## Render へのデプロイ手順

### 1. GitHubにリポジトリを作成してpushする

```bash
cd nikki
git init
git add .
git commit -m "first commit"
git remote add origin https://github.com/ユーザー名/nikki-journal.git
git push -u origin main
```

### 2. Renderで PostgreSQL を作成する

1. [render.com](https://render.com) にサインイン
2. ダッシュボードの「**New +**」→「**PostgreSQL**」
3. 以下を設定：
   - Name: `nikki-db`
   - Database: `nikkidb`
   - User: `nikki`
   - Region: Singapore（日本に近い）
   - Plan: **Free**
4. 「**Create Database**」をクリック
5. 作成後に表示される「**Internal Database URL**」をコピーしておく

### 3. Renderで Web Service を作成する

1. 「**New +**」→「**Web Service**」
2. GitHubリポジトリを連携・選択
3. 以下を設定：

| 項目 | 値 |
|------|----|
| Name | nikki-journal |
| Region | Singapore |
| Branch | main |
| Runtime | Java |
| Build Command | `mvn clean package -DskipTests` |
| Start Command | `java -jar target/nikki-0.0.1-SNAPSHOT.jar` |
| Plan | Free |

4. 「**Environment Variables**」に以下を追加：

| キー | 値 |
|------|----|
| `DATABASE_URL` | （手順2でコピーした Internal Database URL） |
| `SPRING_PROFILES_ACTIVE` | `prod` |

5. 「**Create Web Service**」→ 数分でデプロイ完了

---

## プロファイルの仕組み（重要な学習ポイント）

```
application.properties        ← 常に読み込まれる（共通設定）
      +
application-local.properties  ← profiles=local のとき追加で読み込まれる
      または
application-prod.properties   ← profiles=prod のとき追加で読み込まれる
```

同じキーが複数ファイルにある場合、プロファイル固有のファイルが優先される。

```properties
# application.properties（共通）
spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/nikkidb}

# application-local.properties（ローカルで上書き）
spring.datasource.url=jdbc:h2:mem:nikkidb  ← こちらが優先される
```

---

## URLマッピング一覧

| URL | メソッド | 説明 |
|-----|---------|------|
| `/` または `/today` | GET | 今日の記録ページ |
| `/today` | POST | 今日の記録を保存 |
| `/history` | GET | 一覧・検索（`?keyword=`, `?mood=`, `?year=`, `?month=`） |
| `/entry/{id}` | GET | 詳細ページ |
| `/entry/{id}/edit` | GET | 編集フォーム |
| `/entry/{id}/edit` | POST | 編集を保存 |
| `/entry/{id}/delete` | POST | 削除 |
| `/reflect` | GET | 振り返りページ |

---

## H2 と PostgreSQL の主な違い（学習メモ）

| 項目 | H2 | PostgreSQL |
|------|----|------------|
| データ保持 | 再起動で消える | 永続保存 |
| セットアップ | 不要（JARに含まれる） | インストール or クラウド利用 |
| 用途 | 開発・テスト | 本番 |
| 長文カラム | `@Lob` | `@Column(columnDefinition="TEXT")` |
| コンソール | http://localhost:8080/h2-console | psql コマンド or pgAdmin |

---

## アーキテクチャ（リクエストの流れ）

```
ブラウザ
  ↓ HTTP GET /today
EntryController (@Controller)
  ↓ entryService.findToday()
EntryService (@Service)
  ↓ entryRepository.findByDate(today)
EntryRepository (JpaRepository)
  ↓ SELECT * FROM entry WHERE date = '2026-06-18'
PostgreSQL / H2
  ↑ Entry オブジェクト
EntryService
  ↑ Optional<Entry>
EntryController
  ↓ model.addAttribute("entry", entry)
Thymeleaf today.html
  ↓ HTML生成
ブラウザ（表示）
```
