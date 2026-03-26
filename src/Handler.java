import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

public class Handler {

    private ArrayList<GameObject> objects = new ArrayList<>();
    private ArrayList<GameObject> toRemove = new ArrayList<>();

    public int spd = 6;

    public void tick() {
        for (int i = 0; i < objects.size(); i++) {
            objects.get(i).tick();
        }
        flushRemoves();
    }

    public void render(Graphics g) {
        for (int i = 0; i < objects.size(); i++) {
            objects.get(i).render(g);
        }
    }

    public void clearEnemys() {
        objects.removeIf(obj -> obj.getId() != ID.Player);
        toRemove.clear();
    }

    public void addObject(GameObject object) {
        objects.add(object);
    }

    public void removeObject(GameObject object) {
        toRemove.add(object);
    }

    public void flushRemoves() {
        if (!toRemove.isEmpty()) {
            objects.removeAll(toRemove);
            toRemove.clear();
        }
    }

    public List<GameObject> getObjects() {
        return objects;
    }
}
