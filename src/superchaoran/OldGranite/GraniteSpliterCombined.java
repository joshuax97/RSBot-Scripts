package superchaoran.OldGranite;

import org.powerbot.script.*;
import org.powerbot.script.rt6.*;
import org.powerbot.script.rt6.ClientContext;
import org.powerbot.script.rt6.GeItem;

import java.awt.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Created by chaoran on 5/10/16.
 */
@Script.Manifest(
        name = "Granite Splitter Combined", properties = "author=superchaoran; topic=-2; client=6;",
        description = "Splitting Granite 5kg to 500g and make huge profit off it!"
)
public class GraniteSpliterCombined extends PollingScript<ClientContext>implements PaintListener {

    /*500g, 2kg and 5kg*/
    private static int[] graniteIDs = {6979, 6981, 6983};
    private static final long startTime = System.currentTimeMillis();
    private static String status ="Starting";
    private static int totalCrafts = 0;
    private static GeItem granite500g = new GeItem(graniteIDs[0]);
    private static GeItem granite2kg = new GeItem(graniteIDs[1]);
    private static GeItem granite5kg = new GeItem(graniteIDs[2]);
    public static final Tile BANK_TILE = new Tile(3151, 3479, 0);


    private Npc banker;

    @Override
    public void start() {
        log.info("Find nearest Banker");
        status = "Find nearest Banker";
        banker = ctx.npcs.select().name("Banker").nearest().poll();
        if (!banker.inViewport()) {
            log.info("Banker not in viewport");
            status = "Banker not in viewport";
            ctx.movement.step(banker.tile());
            log.info("Walking to banker");
            status = "Walking to banker";
            if (banker.valid()) {
                log.info("Turn to banker");
                status = "Turn to banker";
                ctx.camera.turnTo(banker);
            } else {
                log.info("Banker not valid");
                status = "Banker not valid";
            }
        }
    }

    @Override
    public void poll() {

        switch (state()) {
            case Bank:

                log.info("Wait for Bank to open");
                status = "Wait for Bank to open";
                Condition.wait(new Condition.Check() {
                    @Override
                    public boolean poll() {
                        return ctx.bank.open();
                    }
                }, 20, 50 * 3);

                log.info("Wait for DepositInventory");
                status = "Wait for DepositInventory";
                ctx.bank.depositInventory();

                log.info("Withdrawing...");
                status = "Withdrawing...";
                ctx.bank.withdraw(graniteIDs[2], 3);

                log.info("Wait for bank close");
                status = "Wait for bank close...";
                Condition.wait(new Condition.Check() {
                    @Override
                    public boolean poll() {
                        return ctx.bank.close();
                    }
                }, 20, 50 * 3);

                break;

            case Split:

                log.info("Open Backpack");
                status = "Open Backpack";
                if(!ctx.hud.opened(Hud.Window.BACKPACK)){
                    ctx.hud.open(Hud.Window.BACKPACK);
                }

                //craft 5kg granite
                log.info("Craft 5kg");
                status = "Craft 5kg";
                ctx.backpack.select().id(graniteIDs[2]).poll().interact("Craft");

                //Confirm
                status = "Confirm crafting";
                if (Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() {
                        return ctx.widgets.component(1370, 20).visible();
                    }
                }, 20, 50*3)) {
                    ctx.input.send(" ");
                }

                //wait for completion
                status = "Wait for crafting 5kg";
                Condition.wait(new Condition.Check() {
                    @Override
                    public boolean poll() {
                        return ctx.backpack.select().count() == 12;
                    }
                }, 20, 50 * 10);

                //craft 2kg granite
                log.info("Craft 2kg");
                status = "Craft 2kg";
                ctx.backpack.select().id(graniteIDs[1]).poll().interact("Craft");

                //Confirm
                status = "Confirm crafting";
                if (Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() {
                        return ctx.widgets.component(1370, 20).visible();
                    }
                }, 20, 50*3)) {
                    ctx.input.send(" ");
                }

                //wait for completion
                status = "Wait for crafting 2kg";
                Condition.wait(new Condition.Check() {
                    @Override
                    public boolean poll() {
                        return ctx.backpack.select().count() == 27;
                    }
                }, 20, 50 * 10);

                log.info("completed");
                status = "Completed a backpack cycle";
                totalCrafts += 3;
                break;
        }
    }

    private GraniteSpliterCombined.State state() {
        if(ctx.backpack.select().count()!=3 || ctx.backpack.select().id(graniteIDs[2]).count() != 3){
            return State.Bank;
        } else {
            return State.Split;
        }
    }

    private enum State {
        Split, Bank
    }

    Font font = new Font("Arial", Font.PLAIN, 10);
    Color background = new Color(0, 0, 0, 150);
    public void repaint(Graphics graphics) {
        int unitProfit = ((int) (1.05* 1.05* granite500g.price *26) + granite2kg.price - granite5kg.price * 3) /3 ;
        graphics.setFont(font);
        graphics.setColor(background);
        graphics.drawRect(0, 0, 200, 100);
        graphics.fillRect(0, 0, 200, 100);
        graphics.setColor(Color.WHITE);
        graphics.getFont();
        graphics.drawString("Granite Splitter Combined", 5, 15);
        int runtime = Integer.parseInt("" + (System.currentTimeMillis()- startTime));
        graphics.drawString("Run time: " + timeFormat(runtime), 105, 15);
        graphics.drawString("Status: " + status, 5, 40);
        graphics.drawString("Total Craft: " + totalCrafts, 5, 53);
        graphics.drawString("Crafts/h: " + (int)((3600000D*totalCrafts) / (System.currentTimeMillis() - startTime)), 5, 68);
        graphics.drawString("Profit/Granite(5kg): " + unitProfit, 105, 53);
        graphics.drawString("Total profit: " + unitProfit*totalCrafts, 105, 68);
        graphics.drawString("Profit/h: " + (int)((3600000D*(unitProfit*totalCrafts)) / (System.currentTimeMillis() - startTime)), 105, 83);
    }

    private String timeFormat(long duration) {
        long days = TimeUnit.MILLISECONDS.toDays(duration);
        long hours = TimeUnit.MILLISECONDS.toHours(duration) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(duration));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration));
        if (days == 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d:%02d:%02d", days, hours, minutes, seconds);
    }


}
