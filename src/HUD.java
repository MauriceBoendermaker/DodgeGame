import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

public class HUD {

    public int bounds = 0;
    public static float HEALTH = 100;
    private float greenValue = 255;

    private int score = 0;
    private int level = 1;
    private int points = 0;

    private static final Font FONT_HUD = new Font("Arial", Font.BOLD, 20);
    private static final Font FONT_LEVEL_BG = new Font("Arial", Font.BOLD, 750);
    private static final Font FONT_SCORE_LARGE = new Font("Arial", Font.BOLD, 60);

    public void tick() {
        HEALTH = Game.clamp(HEALTH, 0, 100 + (bounds / 2));

        greenValue = HEALTH * 2;
        greenValue = Game.clamp(greenValue, 0, 255);

        score++;
        points++;
    }

    public void render(Graphics g) {
        g.setFont(FONT_HUD);
        g.setColor(Color.gray);
        g.fillRect(15, 15, 200 + bounds, 32);

        g.setColor(new Color(75, (int) greenValue, 0));
        g.fillRect(15, 15, (int) HEALTH * 2, 32);

        g.setColor(Color.white);
        g.drawRect(15, 15, 200 + bounds, 32);

        g.drawString("Score: " + score, 15, 96);
        g.drawString("Points: " + points, 15, 80);
        g.drawString("Level: " + level, 15, 64);
        g.drawString("SpaceBar = Shop", 15, 120);
        g.drawString("'ESC' = Exit", 15, 136);
        g.drawString("'P' = Pause", 15, 162);

        g.setFont(FONT_LEVEL_BG);
        g.setColor(new Color(255, 255, 255, 75));
        String levelStr = level <= 9 ? "0" + level : "" + level;
        g.drawString(levelStr, Game.WIDTH / 2 - 375, Game.HEIGHT - 102);

        g.setFont(FONT_SCORE_LARGE);
        g.setColor(Color.white);
        g.drawString("Score: " + score, Game.WIDTH / 2 - 100, 60);
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
