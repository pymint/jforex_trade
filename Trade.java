

import com.dukascopy.api.*;
import com.dukascopy.api.system.IClient;
import com.dukascopy.api.system.ClientFactory;
import com.dukascopy.api.IEngine.OrderCommand;
import com.dukascopy.api.system.ISystemListener;
import static com.dukascopy.api.IOrder.State.*;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.io.File;
import java.util.Scanner;

public class Trade {
 private static final Logger LOGGER = LoggerFactory.getLogger(Trade.class);
 private static String jnlpUrl = "https://www.dukascopy.com/client/demo/jclient/jforex.jnlp";
 //replace with http://platform.dukascopy.com/live/jforex.jnlp for live accounts
 private static String userName = "myusername";
 private static String password = "mypassword";/*
 public static IEngine engine;
 public static IAccount account;
 public static IHistory history;*/
 public static void main(String[] args) throws Exception{

  System.out.println("Starting script");
  final IClient client = ClientFactory.getDefaultInstance();
  client.setSystemListener(new ISystemListener() {
            private int lightReconnects = 3;

            @Override
            public void onStart(long processId) {
                LOGGER.info("Strategy started: " + processId);
            }

            @Override
            public void onStop(long processId) {
                LOGGER.info("Strategy stopped: " + processId);
                if (client.getStartedStrategies().size() == 0) {
                    System.exit(0);
                }
            }

            @Override
            public void onConnect() {
                LOGGER.info("Connected");
                lightReconnects = 3;
            }

            @Override
            public void onDisconnect() {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (lightReconnects > 0) {
                            client.reconnect();
                            --lightReconnects;
                        } else {
                            do {
                                try {
                                    Thread.sleep(60 * 1000);
                                } catch (InterruptedException e) {
                                }
                                try {
                                    if(client.isConnected()) {
                                      System.out.println("Client is connected");
                                        break;
                                    }
                                    client.connect(jnlpUrl, userName, password);
                                } catch (Exception e) {
                                    LOGGER.error(e.getMessage(), e);
                                }
                            } while(!client.isConnected());
                        }
                    }
                };
                new Thread(runnable).start();
            }
        });

  LOGGER.info("Connecting...");
  //connect to the server using jnlp, user name and password
  client.connect(jnlpUrl, userName, password);

  //wait for it to connect
  int i = 10; //wait max ten seconds
  while (i > 0 && !client.isConnected()) {
      Thread.sleep(1000);
      i--;
  }
  if (!client.isConnected()) {
      LOGGER.error("Failed to connect Dukascopy servers");
      System.exit(1);
  }

  //subscribe to the instruments
  Set<Instrument> instruments = new HashSet<Instrument>();
  instruments.add(Instrument.EURJPY);
  instruments.add(Instrument.JPNIDXJPY);
  LOGGER.info("Subscribing instruments...");
  client.setSubscribedInstruments(instruments);

  final long strategyId = client.startStrategy(new IStrategy(){

    public void onStart(IContext context) throws JFException {
      IEngine engine = context.getEngine();
      IHistory history = context.getHistory();
      IAccount account = context.getAccount();
      Instrument instrument;
      int choice;
      Scanner inptsl;
      double slPrice;

      boolean maincond=true;
      while (true){
        System.out.format("\nAccount balance: %s equity: %s\n",account.getBalance(),account.getEquity());
        System.out.println("\n1. Trade 2. Check trades 3. Exit\n");
        Scanner inptmc = new Scanner(System.in);
        int mainch = inptmc.nextInt();
        switch(mainch){
          case 1:
            int side;
            double volume;

            while(true){
             System.out.println("\n1. EURJPY 2. NIKKEI225\n");
             Scanner inptc = new Scanner(System.in);
             choice = inptc.nextInt();
             if (choice == 1 || choice == 2){break;}}
            while(true)
              {System.out.println("\n1. BUY 2. SELL\n");
              Scanner inpts = new Scanner(System.in);
              side = inpts.nextInt();
              if (side == 1 || side == 2){break;}}

            System.out.println("\nVolum:\n");
            Scanner invol = new Scanner(System.in);
            volume = invol.nextDouble()/10;
            if (choice==2){volume=volume/100000;}

            if (choice==1){
              instrument = Instrument.EURJPY;}
            else {instrument = Instrument.JPNIDXJPY;}

            System.out.format("\nSet stop loss. Current price %s\n",history.getLastTick(instrument).getBid());
            inptsl = new Scanner(System.in);
            slPrice = inptsl.nextDouble();


            System.out.println("\nSending order\n");
            IOrder orders;
            if (side==1){
            orders = engine.submitOrder("o"+System.nanoTime(), instrument, OrderCommand.BUY, volume);
            }
            else{
              orders = engine.submitOrder("o"+System.nanoTime(), instrument, OrderCommand.SELL, volume);}
            orders.waitForUpdate(2000, IOrder.State.FILLED);
            orders.setStopLossPrice(slPrice);
            orders.waitForUpdate(2000);
            break;
          case 2:
          for (IOrder order : engine.getOrders()){System.out.println("check");
            if(order.getState() == IOrder.State.FILLED){
              instrument = order.getInstrument();
              String dir = "buy"; if (!order.isLong()){dir="sell";}
              while (true){
                boolean cond = true;
                System.out.format("\n%s order opened at %s has %s pips\n",dir,order.getOpenPrice(),order.getProfitLossInPips());
                System.out.format("%s$ profit\n",order.getProfitLossInUSD());
                System.out.println("\n1. Modify SL 2. Modify TP 3. Close 4.Next trade\n");
                Scanner inptch = new Scanner(System.in);
                choice = inptch.nextInt();
                switch (choice){
                  case 1:
                  System.out.format("\n Modify %s sl. Current price %s\n",order.getStopLossPrice(),history.getLastTick(instrument).getBid());
                  inptsl = new Scanner(System.in);
                  slPrice = inptsl.nextDouble();
                  order.setStopLossPrice(slPrice);
                  order.waitForUpdate(2000);
                  break;
                  case 2:
                  System.out.format("\n Modify %s tp. Current price %s\n",order.getTakeProfitPrice(),history.getLastTick(instrument).getBid());
                  Scanner inpttp = new Scanner(System.in);
                  double tpPrice = inpttp.nextDouble();
                  order.setTakeProfitPrice(tpPrice);
                  order.waitForUpdate(2000);
                  break;
                  case 3:
                  System.out.println("\nclose? yes/no\n");
                  Scanner inptcl = new Scanner(System.in);
                  String close = inptcl.nextLine();
                  if (close.equals("yes")){
                      order.close();
                      order.waitForUpdate(2000);}
                  break;
                  case 4:
                  cond = false;
                  break;
                  default:
                }
              if (cond==false){break;}
              }
            }
          }break;
          case 3: maincond=false;break;
        }
      if (maincond==false){break;}
      }
    System.exit(0);

	}
    public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {}
    public void onTick(Instrument instrument, ITick tick) throws JFException { }
    public void onMessage(IMessage message) throws JFException {    }
    public void onAccount(IAccount account) throws JFException {    }
    public void onStop() throws JFException {    }
  });



 }
}
