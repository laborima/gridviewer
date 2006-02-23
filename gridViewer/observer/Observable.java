package observer;

public abstract interface Observable {
  public void registerInterest(Observer obs);
}
