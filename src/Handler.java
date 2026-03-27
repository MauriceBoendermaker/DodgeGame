import java.awt.Graphics;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Handler {

    private ArrayList<GameObject> objects = new ArrayList<>();
    private HashSet<GameObject> toRemove = new HashSet<>();

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
        objects.removeIf(obj -> obj.getId() != ID.Player && obj.getId() != ID.PlayerProjectile);
        toRemove.clear();
        TrailPool.clear();
    }

    public void addObject(GameObject object) {
        objects.add(object);
    }

    public void removeObject(GameObject object) {
        toRemove.add(object);
    }

    public void flushRemoves() {
        if (!toRemove.isEmpty()) {
            objects.removeIf(toRemove::contains);
            toRemove.clear();
        }
    }

    public List<GameObject> getObjects() {
        return objects;
    }
}
