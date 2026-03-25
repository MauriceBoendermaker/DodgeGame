import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

public class Handler {

    private ArrayList<GameObject> objects = new ArrayList<>();

    public int spd = 6;

    public void tick() {
        for (int i = 0; i < objects.size(); i++) {
            objects.get(i).tick();
        }
    }

    public void render(Graphics g) {
        for (int i = 0; i < objects.size(); i++) {
            objects.get(i).render(g);
        }
    }

    public void clearEnemys() {
        objects.removeIf(obj -> obj.getId() != ID.Player);
    }

    public void addObject(GameObject object) {
        objects.add(object);
    }

    public void removeObject(GameObject object) {
        objects.remove(object);
    }

    public List<GameObject> getObjects() {
        return objects;
    }
}
