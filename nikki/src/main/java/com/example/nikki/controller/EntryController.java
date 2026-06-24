package com.example.nikki.controller;

import com.example.nikki.entity.Entry;
import com.example.nikki.service.EntryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * ★ Controller クラス — プレゼンテーション層
 *
 * 役割:
 *   1. ブラウザからの HTTP リクエストを受け取る
 *   2. Service に処理を依頼する
 *   3. 結果を Model に入れて Thymeleaf テンプレートに渡す
 *   4. どのHTMLテンプレートを表示するか指定する
 *
 * @Controller vs @RestController の違い:
 *   @Controller     → メソッドの戻り値がテンプレート名（HTML を返す）
 *   @RestController → メソッドの戻り値がそのままレスポンスボディ（JSON を返す）
 */
@Controller
// @RequestMapping でこのコントローラー全体のベースURLを指定できる
// （今回は各メソッドに直接書く）
public class EntryController {

    private final EntryService entryService;

    // コンストラクタインジェクション
    public EntryController(EntryService entryService) {
        this.entryService = entryService;
    }

    // ================================================================
    // 今日の記録 画面
    // ================================================================

    /**
     * ★ GET /  または  GET /today → 今日の記録画面を表示する
     *
     * @GetMapping → HTTP GET リクエストを処理する
     * Model       → テンプレートに渡すデータの入れ物
     *               model.addAttribute("key", value) で値を追加すると
     *               Thymeleaf で ${key} として使えるようになる
     */
    @GetMapping({"/", "/today"})
    public String today(Model model) {
        LocalDate today = LocalDate.now();

        // 今日のエントリーを取得（なければ新しい空のEntryを用意）
        Entry entry = entryService.findToday().orElse(new Entry());
        if (entry.getDate() == null) {
            entry.setDate(today);
        }

        // ストリーク（連続記録日数）を計算
        int streak = entryService.calculateStreak();

        // 最近の記録（今日以外）を5件取得
        List<Entry> recentEntries = entryService.findAll().stream()
                .filter(e -> !e.getDate().equals(today))
                .limit(5)
                .toList();

        // Model にデータを追加 → テンプレートで使えるようになる
        model.addAttribute("entry", entry);
        model.addAttribute("streak", streak);
        model.addAttribute("recentEntries", recentEntries);
        model.addAttribute("today", today);

        // ★ return "today" → src/main/resources/templates/today.html を表示
        return "today";
    }

    /**
     * ★ POST /today → 今日の記録を保存する
     *
     * @PostMapping     → HTTP POST リクエストを処理する
     * @ModelAttribute  → フォームの送信データを Entry オブジェクトに自動マッピングする
     *                    例: <input name="body"> → entry.setBody() に自動でセットされる
     * RedirectAttributes → リダイレクト先にデータを渡すための仕組み
     */
    @PostMapping("/today")
    public String saveToday(@ModelAttribute Entry entry,
                            RedirectAttributes redirectAttributes) {
        // 日付が送られてこなかった場合は今日の日付をセット
        if (entry.getDate() == null) {
            entry.setDate(LocalDate.now());
        }

        entryService.save(entry);

        // ★ フラッシュ属性 → リダイレクト後に一度だけ表示できるメッセージ
        // ページをリロードしても再送信されない（PRGパターン: Post-Redirect-Get）
        redirectAttributes.addFlashAttribute("successMessage", "✓ 保存しました");

        // ★ redirect: → リダイレクトする（ブラウザが再度GETリクエストを送る）
        // これにより、ページ更新時のフォーム再送信を防げる
        return "redirect:/today";
    }

    // ================================================================
    // 過去の記録一覧 画面
    // ================================================================

    /**
     * GET /history → 一覧画面を表示する
     *
     * @RequestParam → URLのクエリパラメータ（?keyword=xxx&mood=yyy）を受け取る
     * required = false → パラメータがなくてもエラーにならない
     * defaultValue    → パラメータがない場合のデフォルト値
     */
    @GetMapping("/history")
    public String history(
            @RequestParam(required = false, defaultValue = "") String keyword,
            @RequestParam(required = false, defaultValue = "") String mood,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            Model model) {

        // 検索・フィルタリングした結果を取得
        List<Entry> entries = entryService.search(keyword, mood, year, month);

        // 月フィルタードロップダウン用の年月一覧
        List<YearMonth> yearMonths = entryService.findAllYearMonths();

        model.addAttribute("entries", entries);
        model.addAttribute("yearMonths", yearMonths);
        model.addAttribute("keyword", keyword);
        model.addAttribute("mood", mood);
        model.addAttribute("year", year);
        model.addAttribute("month", month);
        model.addAttribute("totalCount", entries.size());

        return "history";
    }

    // ================================================================
    // 編集画面
    // ================================================================

    /**
     * GET /entry/{id}/edit → 編集画面を表示する
     *
     * @PathVariable → URLの {id} 部分を取得する
     * 例: /entry/3/edit → id = 3
     */
    @GetMapping("/entry/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        // IDで検索。見つからなければ一覧にリダイレクト
        Entry entry = entryService.findById(id)
                .orElse(null);

        if (entry == null) {
            return "redirect:/history";
        }

        model.addAttribute("entry", entry);
        return "edit";
    }

    /**
     * POST /entry/{id}/edit → 編集内容を保存する
     */
    @PostMapping("/entry/{id}/edit")
    public String updateEntry(@PathVariable Long id,
                              @ModelAttribute Entry updated,
                              RedirectAttributes redirectAttributes) {
        entryService.update(id, updated);
        redirectAttributes.addFlashAttribute("successMessage", "✓ 更新しました");

        // 保存後は一覧画面に戻る
        return "redirect:/history";
    }

    // ================================================================
    // 削除
    // ================================================================

    /**
     * POST /entry/{id}/delete → エントリーを削除する
     *
     * 削除は副作用があるので POST（または DELETE）を使う。
     * HTML の <form> は GET と POST しか送れないため、POST で代用する。
     */
    @PostMapping("/entry/{id}/delete")
    public String deleteEntry(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        entryService.delete(id);
        redirectAttributes.addFlashAttribute("successMessage", "削除しました");
        return "redirect:/history";
    }

    // ================================================================
    // 振り返り画面
    // ================================================================

    /**
     * GET /reflect → 振り返り画面を表示する
     */
    @GetMapping("/reflect")
    public String reflect(Model model) {
        LocalDate today = LocalDate.now();
        List<EntryService.ReflectionItem> reflections = entryService.getReflections(today);

        model.addAttribute("reflections", reflections);
        model.addAttribute("today", today);
        return "reflect";
    }

    // ================================================================
    // 詳細表示（モーダルなしのシンプルな詳細ページ）
    // ================================================================

    /**
     * GET /entry/{id} → エントリーの詳細ページを表示する
     */
    @GetMapping("/entry/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Entry entry = entryService.findById(id).orElse(null);
        if (entry == null) return "redirect:/history";

        model.addAttribute("entry", entry);
        return "detail";
    }
}
