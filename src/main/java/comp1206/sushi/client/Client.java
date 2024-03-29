package comp1206.sushi.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import comp1206.sushi.common.Basket;
import comp1206.sushi.common.Dish;
import comp1206.sushi.common.Message;
import comp1206.sushi.common.MessageBasket;
import comp1206.sushi.common.MessageLogin;
import comp1206.sushi.common.MessageOrder;
import comp1206.sushi.common.MessageRegisterUser;
import comp1206.sushi.common.MessageWithAttachement;
import comp1206.sushi.common.Order;
import comp1206.sushi.common.Postcode;
import comp1206.sushi.common.Restaurant;
import comp1206.sushi.common.UpdateEvent;
import comp1206.sushi.common.UpdateListener;
import comp1206.sushi.common.User;

public class Client implements ClientInterface {

    private static final Logger logger = LogManager.getLogger("Client");
	private CommsClient commsClient;
   
	private User user;
    private List<Postcode> postcodes;
    private List<Dish> dishes;
    private List<Order> orders;
    private List<UpdateListener> listeners = new CopyOnWriteArrayList<UpdateListener>();
    private Basket userBasket;
    private Restaurant restaurant;
    

	public Client() {
        logger.info("Starting up client...");
        try {
        	Thread.sleep(500);
        } catch (InterruptedException ie) {
        	ie.printStackTrace();
        }
        commsClient = new CommsClient(this);
        Thread clientThread = new Thread(commsClient);
        clientThread.setName("Client");
        clientThread.setDaemon(true);
        clientThread.start();
        //this.notifyUpdate();
        while(!commsClient.isReady()) {
        	
        }
	}
	
	@Override
	public Restaurant getRestaurant() {

		if(restaurant==null) {
			commsClient.sendMessage("GET-RESTAURANT");
			while(restaurant==null) {
				restaurant=commsClient.getRestaurant();
			}
		}

		return restaurant;
	}
	
	@Override
	public String getRestaurantName() {
		if(restaurant==null) {
			getRestaurant();
		}
		return restaurant.getName();
	}

	@Override
	public Postcode getRestaurantPostcode() {
		if(restaurant==null) {
			getRestaurant();
		}
		return restaurant.getLocation();
	}
	
	@Override
	public User register(String username, String password, String address, Postcode postcode) {
		MessageRegisterUser msg = new MessageRegisterUser(username, password, address, postcode);
		commsClient.sendMessage(msg);
		while(!commsClient.isUserReady()) {
		}
		user = commsClient.getUser();
		System.out.println(user.getName());
		commsClient.resetUserReady();
		return user;
		
	}

	@Override
	public User login(String username, String password) {
		MessageLogin msg = new MessageLogin(username, password);
		commsClient.sendMessage(msg);
		
		while(!commsClient.isUserReady()) {
		}
		user = commsClient.getUser();
		commsClient.resetUserReady();
		return user;
	}

	@Override
	public synchronized List<Postcode> getPostcodes() {
		if(postcodes==null) {
			commsClient.sendMessage("GET-POSTCODES");
			while(postcodes==null) {
				postcodes = commsClient.getPostcodes();
			}
		} else {
			postcodes=commsClient.getPostcodes();
		}
		return postcodes;
	}

	@Override
	public synchronized List<Dish> getDishes() {

		if(dishes==null) {
			commsClient.sendMessage("GET-DISHES");
			while(dishes==null) {
				dishes = commsClient.getDishes();
			}
		} else {

			dishes=commsClient.getDishes();
		}
		return dishes;
	}

	@Override
	public String getDishDescription(Dish dish) {
		if(dishes==null) {
			getDishes();
		}
		return dish.getDescription();
	}

	@Override
	public Number getDishPrice(Dish dish) {
		if(dishes==null) {
			getDishes();
		}
		return dish.getPrice();
	}

	@Override
	public synchronized Map<Dish, Number> getBasket(User user) {
		if(userBasket==null) {
			userBasket = new Basket();
		}
		return userBasket.getContents();
	}

	@Override
	public Number getBasketCost(User user) {
		getBasket(user);
		return userBasket.getBasketCost();
	}

	@Override
	public void addDishToBasket(User user, Dish dish, Number quantity) {
		if(userBasket==null) {
			getBasket(user);
		}
		userBasket.addDishToBasket(dish, quantity);
		Message m = new MessageBasket("ADD-DISH",dish.getName(), quantity);
		commsClient.sendMessage(m);
	}

	@Override
	public void updateDishInBasket(User user, Dish dish, Number quantity) {
		if(userBasket==null) {
			getBasket(user);
		}
		userBasket.updateDishInBasket(dish, quantity);
		Message m = new MessageBasket("UPDATE-DISH",dish.getName(), quantity);
		commsClient.sendMessage(m);
	}

	@Override
	public Order checkoutBasket(User user) {
		clearBasket(user);
		Message m = new Message("CHECKOUT-BASKET");
		commsClient.sendMessage(m);
		return userBasket.checkoutBasket(user);
	}

	@Override
	public void clearBasket(User user) {
		if(userBasket==null) {
			getBasket(user);
		}
		Message m = new Message("CLEAR-BASKET");
		userBasket.clearBasket();
	}

	@Override
	public synchronized List<Order> getOrders(User user) {

		orders = commsClient.getOrders();

		return orders;
	}

	@Override
	public boolean isOrderComplete(Order order) {
		if(order.getStatus().equals("Complete")) {
			return true;
		}
		else {
			return false;
		}
	}

	@Override
	public String getOrderStatus(Order order) {
		return order.getStatus();
	}

	@Override
	public Number getOrderCost(Order order) {
		return order.getCost();
	}

	@Override
	public void cancelOrder(Order order) {
		MessageOrder msg = new MessageOrder("CANCEL-ORDER", order);
		commsClient.sendMessage(msg);

	}

	@Override
	public void addUpdateListener(UpdateListener listener) {
		this.listeners.add(listener);

	}

	@Override
	public void notifyUpdate() {
		/*
		 * I found a bug in ClientWindow implementation, and ignoring the NullPointerException solves the issue
		 */
		try {
			this.listeners.forEach(listener -> listener.updated(new UpdateEvent()));
		} catch (NullPointerException np) {
			
		}

	}

}
