import point.rar.repository.WikiGame;

public class MainFuture {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        WikiGame wikiGame = new WikiGameFutureImpl();
//        var path = wikiGame.play("Алгебра", "Ятаган", 6);
        var path = wikiGame.play("Бакуган", "Библия", 6);
        System.out.printf(path.toString());
        long endTime = System.currentTimeMillis();
        System.out.println("Total execution time: " + (endTime - startTime) + "ms");
    }
}