import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.Random;

public class Spawn {
	
		//Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	
        private Handler handler;
        private HUD hud;
        private Game game;
        private Random r = new Random();
        
        private int scoreKeep = 0;
        
        public Spawn(Handler handler, HUD hud, Game game){
                this.handler = handler;
                this.hud = hud;
                this.game = game;
        }
        
        public void tick() {   
        	
        scoreKeep++;
        
        if(scoreKeep >= 250){ //250
        	scoreKeep = 0;
        	hud.setLevel(hud.getLevel() + 1);
        
        	if(game.diff == 0){ //every 10 levels a Boss lvl
        		if(hud.getLevel() == 2){
                	handler.addObject(new BasicEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.BasicEnemy, handler));
                }else if(hud.getLevel() == 3){
                	handler.addObject(new BasicEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.BasicEnemy, handler));
                }else if(hud.getLevel() == 4){
                	handler.addObject(new FastEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.FastEnemy, handler));
                }else if(hud.getLevel() == 5){
                	handler.addObject(new SmartEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.SmartEnemy, handler));
                }else if(hud.getLevel() == 6){
                	handler.addObject(new FastEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.FastEnemy, handler));
                }else if(hud.getLevel() == 8){
                    handler.addObject(new FastEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.FastEnemy, handler));
                }else if(hud.getLevel() == 10){
                	handler.clearEnemys();
                    handler.addObject(new EnemyBoss((1280 / 2) - 48, -120, ID.EnemyBoss, handler));
                }else if(hud.getLevel() == 15){
                	handler.clearEnemys();
                	handler.addObject(new FastEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.FastEnemy, handler));
                }else if(hud.getLevel() == 17){
                	handler.addObject(new FastEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.FastEnemy, handler));
                }else if(hud.getLevel() == 18){
                	handler.addObject(new BasicEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.BasicEnemy, handler));
                }else if(hud.getLevel() == 20){
                	handler.addObject(new BasicEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.BasicEnemy, handler));
                }else if(hud.getLevel() == 22){
                	handler.addObject(new BasicEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.BasicEnemy, handler));
                }else if(hud.getLevel() == 25){
                	handler.addObject(new SmartEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.SmartEnemy, handler));
                	}
        		}
        
        	else if(game.diff == 1){ //every 15 Levels a Boss lvl
        		if(hud.getLevel() == 2){
        			handler.addObject(new HardEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.HardEnemy, handler));
                }else if(hud.getLevel() == 3){ //remove, add & optimize it later
                	handler.addObject(new SmartEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.SmartEnemy, handler));
                }else if(hud.getLevel() == 4){
                	handler.addObject(new BasicEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.BasicEnemy, handler));
                }else if(hud.getLevel() == 5){
                	handler.addObject(new BasicEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.BasicEnemy, handler));
                }else if(hud.getLevel() == 6){
                	handler.addObject(new HardEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.HardEnemy, handler));
        		}else if(hud.getLevel() == 15){
        			handler.clearEnemys();
        			handler.addObject(new EnemyBoss((1280 / 2) - 48, -120, ID.EnemyBoss, handler));
        		}
        	}
        	
        	else if(game.diff == 2)
    		{
    		if(hud.getLevel() == 2){
    			handler.addObject(new FastEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.FastEnemy, handler));
            }else if(hud.getLevel() == 3){
            	handler.addObject(new FastEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.FastEnemy, handler));
            }else if(hud.getLevel() == 4){
            	handler.addObject(new SmartEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.SmartEnemy, handler));
            }else if(hud.getLevel() == 5){
            	handler.addObject(new HardEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.HardEnemy, handler));
            }else if(hud.getLevel() == 7){
            	handler.addObject(new HardEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.HardEnemy, handler));
            }else if(hud.getLevel() == 8){
            	handler.addObject(new BasicEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.BasicEnemy, handler));
            }else if(hud.getLevel() == 10){
            	handler.addObject(new FastEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.FastEnemy, handler));
            }else if(hud.getLevel() == 12){
            	handler.addObject(new BasicEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.BasicEnemy, handler));
    		}else if(hud.getLevel() == 15){
    			handler.clearEnemys();
    			handler.addObject(new EnemyBoss((1280 / 2) - 48, -120, ID.EnemyBoss, handler));
    		}else if(hud.getLevel() == 20){
            	handler.addObject(new HardEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.HardEnemy, handler));
            }
    	}
        	
        	else if(game.diff == 4)
    		{
        	if(hud.getLevel() == 2){
    			handler.addObject(new FastEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.FastEnemy, handler));
            }else if(hud.getLevel() == 3){
            	handler.addObject(new HardEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.HardEnemy, handler));
            }else if(hud.getLevel() == 4){
    			handler.addObject(new SmartEnemy(r.nextInt(1280 - 50), r.nextInt(720 - 50), ID.SmartEnemy, handler));    			
            	}
    		}
        }
    }      
}
