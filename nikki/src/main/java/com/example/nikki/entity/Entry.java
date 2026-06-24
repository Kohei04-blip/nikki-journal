package com.example.nikki.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ★ Entity クラス — データベースのテーブルに対応するクラス
 *
 * @Entity をつけると、JPA がこのクラスを見て
 * 自動的に "entry" テーブルを作成してくれる。
 *
 * フィールド = カラム（列）に対応する。
 */
@Entity
// @Table でテーブル名を明示的に指定できる（省略するとクラス名が使われる）
@Table(name = "entry")
public class Entry {

    /**
     * ★ 主キー（Primary Key）
     *
     * @Id       → このフィールドが主キーであることを示す
     * @GeneratedValue → 値を自動生成する
     *   strategy = IDENTITY → DBのAUTO_INCREMENT に任せる
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 記録した日付（例: 2026-06-18）
     *
     * @Column(unique = true) → 同じ日付のレコードは1つだけ許可
     * LocalDate は時刻なしの日付型。日記は1日1件なので Date で十分。
     */
    @Column(nullable = false, unique = true)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    /**
     * 今日のこと（日記本文）
     *
     * ★ @Column(columnDefinition = "TEXT") について
     *
     * H2 では @Lob で長文を保存できたが、PostgreSQL では
     * @Lob が BYTEA（バイナリ型）にマッピングされてしまい、
     * 文字列として扱えなくなる問題がある。
     *
     * PostgreSQL で長いテキストを保存するには TEXT 型を使う。
     * columnDefinition = "TEXT" と書くことで、DDL（CREATE TABLE文）に
     * そのまま "TEXT" と書き込まれる。
     *
     * TEXT型 vs VARCHAR型:
     *   VARCHAR(n) → 最大文字数を指定（例: VARCHAR(255)）
     *   TEXT       → 文字数制限なし（日記のような長文に適している）
     */
    @Column(columnDefinition = "TEXT")
    private String body;

    /** メモ欄 */
    @Column(columnDefinition = "TEXT")
    private String memo;

    /** 気づき欄 */
    @Column(columnDefinition = "TEXT")
    private String insight;

    /** 悩み欄 */
    @Column(columnDefinition = "TEXT")
    private String worry;

    /**
     * 気分（happy / neutral / sad / angry / tired）
     * String で保存する。Enum にすることもできるが、今回はシンプルに。
     */
    private String mood;

    /**
     * 作成日時（最初に保存した日時）
     *
     * updatable = false → 一度セットしたら更新不可にする
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** 更新日時（編集するたびに更新される） */
    private LocalDateTime updatedAt;

    /**
     * ★ @PrePersist — INSERT 直前に自動実行されるメソッド
     * createdAt と updatedAt の初期値をセットする。
     * 呼び出し忘れがなく、安全に日時を記録できる。
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * ★ @PreUpdate — UPDATE 直前に自動実行されるメソッド
     * 編集保存のたびに updatedAt を現在時刻に更新する。
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ----------------------------------------------------------------
    // ★ コンストラクタ
    // JPA は引数なしコンストラクタ（デフォルトコンストラクタ）を必要とする。
    // ----------------------------------------------------------------
    public Entry() {}

    // ----------------------------------------------------------------
    // ★ Getter / Setter
    // フィールドは private なので、外部からアクセスするには必要。
    // IDEで自動生成するのが一般的（Lombokを使えば @Data 1行でも書ける）。
    // ----------------------------------------------------------------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }

    public String getInsight() { return insight; }
    public void setInsight(String insight) { this.insight = insight; }

    public String getWorry() { return worry; }
    public void setWorry(String worry) { this.worry = worry; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    /**
     * ★ 気分の絵文字を返すヘルパーメソッド
     * Thymeleaf テンプレートから ${entry.moodEmoji} で呼び出せる。
     * ロジックをテンプレートに書かずに済む。
     */
    public String getMoodEmoji() {
        if (mood == null) return "";
        return switch (mood) {
            case "happy"   -> "😊";
            case "neutral" -> "😐";
            case "sad"     -> "😔";
            case "angry"   -> "😤";
            case "tired"   -> "😴";
            default        -> "";
        };
    }

    /**
     * 気分のラベルを返すヘルパーメソッド
     */
    public String getMoodLabel() {
        if (mood == null) return "";
        return switch (mood) {
            case "happy"   -> "良い気分";
            case "neutral" -> "普通";
            case "sad"     -> "辛い";
            case "angry"   -> "もやもや";
            case "tired"   -> "疲れた";
            default        -> "";
        };
    }
}
