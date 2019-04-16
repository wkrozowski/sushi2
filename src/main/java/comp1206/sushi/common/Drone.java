package comp1206.sushi.common;

import com.esotericsoftware.kryonet.Server;

import comp1206.sushi.common.Drone;
import comp1206.sushi.server.ServerInterface;

public class Drone extends Model implements Runnable {

	private Number speed;
	private Number progress;
	
	private Number capacity;
	private Number battery;
	
	private String status;
	
	private Postcode source;
	private Postcode destination;

	private volatile boolean shutdown = false;
	private Thread threadInstance; 
	private StockManagement stockManagement;
	private ServerInterface server;
	
	public synchronized void setThreadInstance(Thread threadInstance) {
		this.threadInstance = threadInstance;
	}
	
	public synchronized Thread getThreadInstace() {
		return this.threadInstance;
	}
	
	public void setServer(ServerInterface server) {
		this.server = server;
	}
	
	public void setStockManagement(StockManagement stockManagement) {
		this.stockManagement = stockManagement;
	}
	
	public Drone(Number speed) {
		this.setSpeed(speed);
		this.setCapacity(1);
		this.setBattery(100);
		this.setStatus("Idle");
	}

	public Number getSpeed() {
		return speed;
	}

	
	public Number getProgress() {
		return progress;
	}
	
	public synchronized void setProgress(Number progress) {
		this.progress = progress;
	}
	
	public synchronized void setSpeed(Number speed) {
		this.speed = speed;
	}
	
	@Override
	public String getName() {
		return "Drone (" + getSpeed() + " speed)";
	}

	public synchronized Postcode getSource() {
		return source;
	}

	public synchronized void setSource(Postcode source) {
		this.source = source;
	}

	public synchronized Postcode getDestination() {
		return destination;
	}

	public synchronized void setDestination(Postcode destination) {
		this.destination = destination;
	}

	public Number getCapacity() {
		return capacity;
	}

	public synchronized void setCapacity(Number capacity) {
		this.capacity = capacity;
	}

	public Number getBattery() {
		return battery;
	}

	public synchronized void setBattery(Number battery) {
		this.battery = battery;
	}

	public synchronized String getStatus() {
		return status;
	}

	public synchronized void setStatus(String status) {
		notifyUpdate("status",this.status,status);
		this.status = status;
	}

	public void shutdown() {
		shutdown=true;
	}
	
	public void fly(Postcode source, Postcode destination, Number route) {
		this.setSource(source);
		this.setDestination(destination);
		this.setStatus("Flying");
		Integer soFar = 0;
		while(soFar<route.intValue()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ie) {
				
			}
			soFar += this.getSpeed().intValue();
			this.setProgress(Math.round(soFar*100/route.intValue()));
		}
		this.setStatus("Idle");
	}
	
	
	public void deliverIngredient(Ingredient i) {
		Supplier supplier = i.getSupplier();
		Number distance = supplier.getDistance();
		Postcode source = server.getRestaurantPostcode();
		Postcode destination = supplier.getPostcode();
		fly(source, destination, distance);
		fly(destination, source, distance);
	}
	
	
	@Override
	public void run() {
		while(!shutdown) {
			//If we can restock ingredient, do it
			Ingredient nextIngredient = stockManagement.getNextIngredient();
			if(nextIngredient!=null) {
				stockManagement.notifyRestocking(nextIngredient);
				System.out.println("Restocking: " + nextIngredient);
				deliverIngredient(nextIngredient);
				stockManagement.restockIngredient(nextIngredient);
				stockManagement.notifyRestockingFinished(nextIngredient);
			}
			
			//If we can deliver order, do it
			Order nextOrder = stockManagement.getNextOrder();
			if(nextOrder!=null)
			{
				System.out.println("delivering order");
			}
			
			//Otherwise, do nothing and wait
			this.setStatus("Idle");

		}
		
	}
	
}
