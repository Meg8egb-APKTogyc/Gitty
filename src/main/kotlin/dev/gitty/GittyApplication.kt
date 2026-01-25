package dev.gitty

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableAsync

@SpringBootApplication
@EnableAsync
class GittyApplication

fun main(args: Array<String>) {
	runApplication<GittyApplication>(*args)
}