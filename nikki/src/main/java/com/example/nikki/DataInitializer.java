package com.example.nikki;

import com.example.nikki.entity.Entry;
import com.example.nikki.repository.EntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * ★ DataInitializer — アプリ起動時にデモデータを投入するクラス
 *
 * CommandLineRunner を実装すると、Spring Boot 起動完了後に
 * run() メソッドが自動で呼ばれる。
 *
 * ★ @Profile("local") — このクラスはローカル環境でのみ有効
 *
 * プロファイルの仕組み:
 *   @Profile("local")  → spring.profiles.active=local のときだけ動く
 *   @Profile("prod")   → spring.profiles.active=prod のときだけ動く
 *   @Profile("!prod")  → prod 以外のときに動く（! = NOT）
 *
 * 本番環境でデモデータが入ってしまうのを防ぐために @Profile("local") を使う。
 */
@Component
@Profile("local")  // ★ ローカル環境でのみ実行される
public class DataInitializer implements CommandLineRunner {

    // ★ Logger — ログ出力のためのクラス
    // System.out.println より推奨される。ログレベル（DEBUG/INFO/WARN/ERROR）で制御できる。
    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final EntryRepository entryRepository;

    public DataInitializer(EntryRepository entryRepository) {
        this.entryRepository = entryRepository;
    }

    /**
     * ★ CommandLineRunner.run() — アプリ起動後に自動実行される
     *
     * すでにデータがある場合はスキップする（冪等性: 何度実行しても同じ結果）。
     */
    @Override
    public void run(String... args) throws Exception {
        // すでにデータがあればスキップ
        if (entryRepository.count() > 0) {
            log.info("デモデータは既に存在します。スキップします。");
            return;
        }

        log.info("デモデータを投入します...");

        LocalDate today = LocalDate.now();

        // ★ デモデータを配列で定義して一括登録
        Object[][] demoData = {
            // { daysAgo, body, memo, insight, worry, mood }
            {1, "今日は久しぶりに早起きできた。朝の空気が気持ちよくて、近所を少し歩いた。小さなことだけど、なんだか一日がうまく回った気がする。",
               "明日：歯医者の予約を確認する\n先週読みかけの本を読み返す", "", "", "happy"},
            {2, "会議が続いて疲れた。でも夕飯においしいカレーを作ったら気分が少し楽になった。自炊って大事だと思う。",
               "", "自炊すると気持ちが落ち着くことに気づいた。忙しいときこそ料理をする価値があるかも。",
               "来週のプレゼンが不安。準備が追いついていない気がする。", "tired"},
            {5, "友人から突然連絡が来た。久しぶりすぎて最初は戸惑ったけど、話してみたら楽しかった。",
               "友人おすすめの映画：「ドライブ・マイ・カー」",
               "人とつながることの大切さを実感した。疎遠になっていても、また話せるものだ。", "", "happy"},
            {8, "特に何もなかった日。こういう平穏な日を記録しておくことも大事だと思って書いた。",
               "", "", "なんとなく将来のことが心配。具体的ではないけど、漠然とした不安がある。", "neutral"},
            {14, "先週から気になっていたプロジェクトがようやく前進した。自分を信じて続けてよかった。",
                "参考にしたリソース：デザインシステムの記事をブックマーク",
                "焦らず続けることが一番の近道だと改めて感じた。", "", "happy"},
            {21, "少し落ち込んでいる。理由はよくわからないけど、こういう日もある。明日は違うかもしれない。",
                "", "", "人間関係で少し引っかかることがあった。自分の受け取り方の問題なのか、まだわからない。", "sad"},
            {35, "新しいことを始めた月。緊張したけど、やってみると案外なんとかなるものだ。",
                "新しく始めたこと：朝のストレッチ習慣",
                "「やる前の恐怖」と「やってみた後の感覚」は、いつも全然違う。", "", "neutral"},
        };

        for (Object[] data : demoData) {
            Entry entry = new Entry();
            entry.setDate(today.minusDays((Integer) data[0]));
            entry.setBody((String) data[1]);
            entry.setMemo((String) data[2]);
            entry.setInsight((String) data[3]);
            entry.setWorry((String) data[4]);
            entry.setMood((String) data[5]);
            entryRepository.save(entry);
        }

        log.info("デモデータ {} 件を投入しました。", demoData.length);
    }
}
