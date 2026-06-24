package com.example.nikki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ★ アプリケーションの起動クラス
 *
 * Spring Boot アプリはここから始まる。
 * main メソッドを実行するだけで、組み込みTomcatが起動し
 * http://localhost:8080 でアクセスできるようになる。
 */

// ★ @SpringBootApplication は3つのアノテーションをまとめたもの:
//   @Configuration       → このクラスがBean定義のソースになる
//   @EnableAutoConfiguration → 依存関係を見て設定を自動化する
//   @ComponentScan       → このパッケージ以下の @Component を自動検出する
@SpringBootApplication
public class NikkiApplication {

    public static void main(String[] args) {
        // SpringApplication.run() がサーバーを起動する
        // 第1引数: 起動の起点となるクラス
        // 第2引数: コマンドライン引数（そのまま渡す）
        SpringApplication.run(NikkiApplication.class, args);
    }
}
