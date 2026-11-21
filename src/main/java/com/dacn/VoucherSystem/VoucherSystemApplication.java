package com.dacn.VoucherSystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VoucherSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(VoucherSystemApplication.class, args);
	}
}
