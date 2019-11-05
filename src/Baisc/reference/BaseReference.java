package Baisc.reference;

import lombok.Data;

/**
 * @Author: Heiku
 * @Date: 2019/11/5
 */

public class BaseReference {

    @Override
    protected void finalize() throws Throwable {
        System.out.println("BaseReference's finalize called");
        super.finalize();
    }

    @Override
    public String toString() {
        return "I am BaseReference";
    }
}
