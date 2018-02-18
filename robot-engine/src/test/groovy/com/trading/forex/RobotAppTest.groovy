package com.trading.forex

import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Profile
import org.springframework.test.context.junit4.SpringRunner

@RunWith(SpringRunner)
@Profile("dev")
//@SpringBootTest(classes = RobotApp)
class RobotAppTest {

    @Test
    void testConf(){
        // Run app
    }

}