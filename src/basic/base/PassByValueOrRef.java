package basic.base;

import lombok.Data;
import lombok.ToString;
import org.junit.jupiter.api.Test;

/**
 * Pass By Value Or Pass By Reference
 *
 * @Author: Heiku
 * @Date: 2019/12/10
 */
public class PassByValueOrRef {

    @Test
    public void baseValueTest(){
        PassByValueOrRef ref = new PassByValueOrRef();
        // m -> actual parameter
        int n = 10;
        ref.sout(n);
        System.out.println("value didn't change: " + n);
    }

    // n -> formal parameter
    private void sout(int n){
        n = 20;
        System.out.println(n);
    }


    @Test
    public void refTest(){
        User user = new User();
        user.setName("Jacky");
        user.setGender("man");

        change(user);
        // it seems like pass by ref, the "user" changed
        System.out.println(user);
    }

    private void change(User no){
        // if Java is ref pass, and "user ref" still point to heap "user address"
        // but it didn't
        // so we should focus on the "if we change ref(ref = new ref), point to address would change ? "
        // if it change, is ref1 -> address1,  ref1 -> address2, it must pass by ref
        // if not, is ref1 -> address1, ref2 -> address2, copy the ref, it must pass by value

        // user -> address1, no -> address2, it must not be pass by ref
        no = new User();
        no.setName("Joe");
        no.setGender("girl");
        System.out.println(no);

    }

    // this time, it would copy user(actual param) address1 to variable(no)
    // so, user -> address1 & no -> address1
    /*private void change(User no){
        no.setName("Joe");
        no.setGender("girl");
        System.out.println(no);
    }*/
}

@Data
@ToString
class User{
    private String name;
    private String gender;
}

