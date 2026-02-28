package com.paynetSystem.paynetSystemBank;

import com.paynetSystem.paynetSystemBank.auth_users.entity.User;
import com.paynetSystem.paynetSystemBank.enums.NotificationType;
import com.paynetSystem.paynetSystemBank.notification.dtos.NotificationDTO;
import com.paynetSystem.paynetSystemBank.notification.services.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@RequiredArgsConstructor
@SpringBootApplication
public class PaynetSystemBankApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaynetSystemBankApplication.class, args);
	}

	private final NotificationService notificationService;

	// Email TEST
	@Bean
	CommandLineRunner runner(){
		return args -> {
			NotificationDTO notificationDTO = NotificationDTO.builder()
					.recipient("jumanazardilmurod@mail.ru")
					.subject("Привет, тестовое письмо")
					.body("Здравствуйте, это тестовое письмо.")
					.type(NotificationType.EMAIL)
					.build();

			notificationService.sendEmail(notificationDTO,new User());

		};
	}

}
