package com.trading.forex.economic.configuration;

import org.openqa.selenium.chrome.ChromeDriver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;
//@Configuration
public class SileniumConfig {


    @Bean
    public ChromeDriver chromeDriver(){
        String exePath = "C:\\Users\\utilisateur\\Downloads\\olddownlaod\\chromedriver_win32\\chromedriver.exe";
        System.setProperty("webdriver.chrome.driver", exePath);
        final ChromeDriver chromeDriver= new ChromeDriver();
        chromeDriver.manage().timeouts().implicitlyWait(3, TimeUnit.MINUTES);
        return chromeDriver;
    }
}
