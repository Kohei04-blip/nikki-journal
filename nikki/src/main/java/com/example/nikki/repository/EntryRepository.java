package com.example.nikki.repository;

import com.example.nikki.entity.Entry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ★ Repository インターフェース — データベースアクセス層
 *
 * JpaRepository を継承するだけで、基本的なCRUD操作が自動的に使えるようになる。
 *
 *   JpaRepository<Entry, Long> の意味:
 *     Entry → 操作対象のEntityクラス
 *     Long  → 主キーの型（Entry の id フィールドの型）
 *
 * 自動で使えるようになる主なメソッド:
 *   save(entry)        → INSERT または UPDATE
 *   findById(id)       → SELECT WHERE id = ?
 *   findAll()          → SELECT * FROM entry
 *   delete(entry)      → DELETE WHERE id = ?
 *   count()            → SELECT COUNT(*)
 */
@Repository  // ★ Springにこれがリポジトリだと伝えるアノテーション（省略可だが明示が推奨）
public interface EntryRepository extends JpaRepository<Entry, Long> {

    /**
     * ★ メソッド名による自動クエリ生成（Spring Data JPAの強力な機能）
     *
     * findBy + フィールド名 で書くだけで、Spring が自動的に
     * SELECT * FROM entry WHERE date = ? というSQLを生成してくれる。
     *
     * Optional<Entry> → 見つかればEntry、なければ空を返す（nullを避けられる）
     */
    Optional<Entry> findByDate(LocalDate date);

    /**
     * 全件を日付の降順で取得する
     * OrderByDateDesc → ORDER BY date DESC に変換される
     */
    List<Entry> findAllByOrderByDateDesc();

    /**
     * ★ @Query — 自分でJPQLを書くカスタムクエリ
     *
     * JPQL（Java Persistence Query Language）はSQLに似ているが、
     * テーブル名ではなくクラス名・フィールド名を使う。
     *
     * LOWER() で小文字に変換して大文字小文字を無視した検索にしている。
     * CONCAT('%', :keyword, '%') → LIKE %キーワード% 検索
     * :keyword → @Param("keyword") で渡した値に置き換えられる
     */
    @Query("SELECT e FROM Entry e WHERE " +
           "LOWER(e.body) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.memo) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.insight) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(e.worry) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY e.date DESC")
    List<Entry> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 気分でフィルタリング（日付降順）
     */
    List<Entry> findByMoodOrderByDateDesc(String mood);

    /**
     * 特定の日付範囲内のエントリーを取得
     * Between → WHERE date BETWEEN :start AND :end
     */
    List<Entry> findByDateBetweenOrderByDateDesc(LocalDate start, LocalDate end);

    /**
     * 特定の年月のエントリーを取得するカスタムクエリ
     * YEAR(), MONTH() はJPQLで使える日付関数
     */
    @Query("SELECT e FROM Entry e WHERE YEAR(e.date) = :year AND MONTH(e.date) = :month ORDER BY e.date DESC")
    List<Entry> findByYearAndMonth(@Param("year") int year, @Param("month") int month);

    /**
     * 直近N日間にエントリーが存在する日付一覧を取得
     * 連続記録日数（ストリーク）の計算に使う
     */
    @Query("SELECT e.date FROM Entry e WHERE e.date >= :since ORDER BY e.date DESC")
    List<LocalDate> findDatesAfter(@Param("since") LocalDate since);
}
