package net.qihoo.guessthepattern;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

/**
 * Quiz Verse 应用入口。
 * <p>包名 {@code guessthepattern} 为历史遗留；Maven 工程已更名为 {@code quiz-verse}。</p>
 */
@SpringBootApplication
@ServletComponentScan
@EnableWebMvc
public class GuessThePatternApplication {

	public static void main(String[] args) {
		SpringApplication.run(GuessThePatternApplication.class, args);
	}

}
