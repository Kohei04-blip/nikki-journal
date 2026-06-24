package com.example.nikki.service;

import com.example.nikki.entity.Entry;
import com.example.nikki.repository.EntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ★ Service クラス — ビジネスロジック層
 *
 * アーキテクチャの役割分担:
 *   Controller → 「HTTPリクエストを受け取り、Serviceに仕事を依頼する」
 *   Service    → 「ビジネスロジック（アプリの本質的な処理）を担う」  ← ここ
 *   Repository → 「DBへの読み書きを担う」
 *
 * なぜ Controller に直接 Repository を使わないのか？
 *   → ロジックが複雑になったとき（複数のRepositoryを組み合わせるなど）に
 *     Controller が肥大化してしまうから。層を分けることで保守しやすくなる。
 */
@Service  // ★ Spring に「これはサービスクラスだ」と伝える。@Component の一種。
public class EntryService {

    /**
     * ★ コンストラクタインジェクション（推奨される依存性注入の方法）
     *
     * @Autowired をフィールドに付ける方法もあるが、
     * コンストラクタインジェクションのほうがテストしやすく推奨される。
     * コンストラクタが1つだけの場合、@Autowired は省略できる。
     */
    private final EntryRepository entryRepository;

    public EntryService(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    // ----------------------------------------------------------------
    // 今日のエントリー操作
    // ----------------------------------------------------------------

    /**
     * 今日のエントリーを取得する
     * Optional → 今日の記録がなければ empty が返る
     */
    public Optional<Entry> findToday() {
        return entryRepository.findByDate(LocalDate.now());
    }

    /**
     * エントリーを保存または更新する
     *
     * ★ @Transactional — トランザクション管理
     * このメソッドの実行中に例外が起きると、DB操作がすべてロールバックされる。
     * 「全部成功」か「全部失敗」かを保証する重要なアノテーション。
     */
    @Transactional
    public Entry save(Entry entry) {
        // 同じ日付のエントリーがすでにあれば、IDを引き継いで更新にする
        entryRepository.findByDate(entry.getDate()).ifPresent(existing -> {
            entry.setId(existing.getId());
        });
        return entryRepository.save(entry);
    }

    /**
     * IDでエントリーを取得する
     */
    public Optional<Entry> findById(Long id) {
        return entryRepository.findById(id);
    }

    /**
     * 特定の日付のエントリーを取得する
     */
    public Optional<Entry> findByDate(LocalDate date) {
        return entryRepository.findByDate(date);
    }

    // ----------------------------------------------------------------
    // 一覧・検索
    // ----------------------------------------------------------------

    /**
     * 全エントリーを日付降順で取得する
     */
    public List<Entry> findAll() {
        return entryRepository.findAllByOrderByDateDesc();
    }

    /**
     * キーワード検索（本文・メモ・気づき・悩みを横断）
     * keyword が空なら全件返す
     */
    public List<Entry> search(String keyword, String mood, Integer year, Integer month) {
        List<Entry> result;

        // キーワードで絞り込み
        if (keyword != null && !keyword.isBlank()) {
            result = entryRepository.searchByKeyword(keyword.trim());
        } else {
            result = entryRepository.findAllByOrderByDateDesc();
        }

        // 気分で絞り込み（★ Stream API でフィルタリング）
        // stream() → コレクションをストリームに変換
        // filter() → 条件に合う要素だけ残す
        // collect() → ストリームをリストに戻す
        if (mood != null && !mood.isBlank()) {
            final String moodFilter = mood;
            result = result.stream()
                    .filter(e -> moodFilter.equals(e.getMood()))
                    .collect(Collectors.toList());
        }

        // 年月で絞り込み
        if (year != null && month != null) {
            final int y = year, m = month;
            result = result.stream()
                    .filter(e -> e.getDate().getYear() == y && e.getDate().getMonthValue() == m)
                    .collect(Collectors.toList());
        }

        return result;
    }

    /**
     * 記録のある年月一覧を取得する（月フィルターのドロップダウン用）
     */
    public List<YearMonth> findAllYearMonths() {
        return entryRepository.findAllByOrderByDateDesc().stream()
                .map(e -> YearMonth.of(e.getDate().getYear(), e.getDate().getMonth()))
                .distinct()
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    // 編集・削除
    // ----------------------------------------------------------------

    /**
     * エントリーを更新する
     */
    @Transactional
    public Entry update(Long id, Entry updated) {
        // findById が空なら例外を投げる（存在しないIDへの更新を防ぐ）
        Entry existing = entryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + id));

        // 既存エントリーに新しい値をセットして保存
        existing.setBody(updated.getBody());
        existing.setMemo(updated.getMemo());
        existing.setInsight(updated.getInsight());
        existing.setWorry(updated.getWorry());
        existing.setMood(updated.getMood());
        // @PreUpdate により updatedAt が自動更新される

        return entryRepository.save(existing);
    }

    /**
     * エントリーを削除する
     */
    @Transactional
    public void delete(Long id) {
        entryRepository.deleteById(id);
    }

    // ----------------------------------------------------------------
    // 振り返り
    // ----------------------------------------------------------------

    /**
     * 振り返り用：指定日から N日前 のエントリーを探す
     * ±3日以内にあれば「近い日付」として返す（柔軟な振り返り）
     */
    public Optional<Entry> findReflection(LocalDate base, int daysAgo) {
        LocalDate target = base.minusDays(daysAgo);

        // まず完全一致を探す
        Optional<Entry> exact = entryRepository.findByDate(target);
        if (exact.isPresent()) return exact;

        // ±3日の範囲で探す
        List<Entry> nearby = entryRepository.findByDateBetweenOrderByDateDesc(
                target.minusDays(3), target.plusDays(3));

        return nearby.isEmpty() ? Optional.empty() : Optional.of(nearby.get(0));
    }

    /**
     * 振り返りデータをまとめて返す
     * キー: "1週間前" などのラベル / 値: Entry
     */
    public List<ReflectionItem> getReflections(LocalDate base) {
        List<ReflectionItem> items = new ArrayList<>();

        int[] daysAgo  = {7, 30, 90, 180, 365};
        String[] labels = {"1週間前", "1ヶ月前", "3ヶ月前", "半年前", "1年前"};

        for (int i = 0; i < daysAgo.length; i++) {
            final String label = labels[i];
            findReflection(base, daysAgo[i])
                    .ifPresent(e -> items.add(new ReflectionItem(label, e)));
        }
        return items;
    }

    // ----------------------------------------------------------------
    // ストリーク（連続記録日数）計算
    // ----------------------------------------------------------------

    /**
     * 今日から遡って連続記録日数を計算する
     */
    public int calculateStreak() {
        // 直近100日分の記録された日付を取得
        List<LocalDate> dates = entryRepository.findDatesAfter(LocalDate.now().minusDays(100));

        // ★ Stream で日付のSetを作成（検索を O(1) にするため）
        Set<LocalDate> dateSet = dates.stream().collect(Collectors.toSet());

        int streak = 0;
        LocalDate check = LocalDate.now();

        // 今日から1日ずつ遡って、記録がある限りカウントする
        while (dateSet.contains(check)) {
            streak++;
            check = check.minusDays(1);
        }
        return streak;
    }

    /**
     * ★ 内部クラス（静的ネストクラス）
     * 振り返りのラベルとエントリーをセットで持つためのデータクラス。
     * Thymeleaf テンプレートに渡すのに便利。
     */
    public static class ReflectionItem {
        private final String label;
        private final Entry entry;

        public ReflectionItem(String label, Entry entry) {
            this.label = label;
            this.entry = entry;
        }

        public String getLabel() { return label; }
        public Entry getEntry() { return entry; }
    }
}
