package com.example.filelab;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.filelab.mapper")
public class FilelabApplication {

	public static void main(String[] args) {
		SpringApplication.run(FilelabApplication.class, args);
	}

}
