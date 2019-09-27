package spring.inject;

import org.springframework.beans.factory.annotation.Autowired;
import spring.base.BaseService;

/**
 * @Author: Heiku
 * @Date: 2019/9/27
 */
public class InjectBeanTest {

    @Autowired
    private static BaseService baseService;

    public static void main(String[] args) {

    }
}
