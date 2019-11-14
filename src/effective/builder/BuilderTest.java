package effective.builder;

/**
 * @Author: Heiku
 * @Date: 2019/11/14
 */
public class BuilderTest {
    public static void main(String[] args) {
        NutritionFacts nut = new NutritionFacts.Builder(240, 8)
                .calories(100)
                .sodium(35)
                .carbohydrate(27)
                .build();
        System.out.println(nut.toString());
    }
}
