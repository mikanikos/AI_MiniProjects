import logist.topology.Topology.City;

/*
 * A state for the value iteration
 * We can be in a city from and either being offered a task to a city
 * or not being offered a task (to is null in this case)
 */
public class ReactiveState {
	private City from;
	private City to; //If null then no task

	public ReactiveState(City from, City to) {
		this.from = from;
		this.to = to;
	}
	
	public City getFrom() {
		return from;
	}
	
	public City getTo() {
		return to;
	}
	
	@Override
	public int hashCode() {
        int result = 17;
        result = 7 * result + from.hashCode();
        result = 7 * result + (to == null ? 0 : to.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof ReactiveState)) {
            return false;
        }

        ReactiveState user = (ReactiveState) obj;

        return user.from.equals(from) &&
                (user.to == null && to == null || user.to.equals(to));
	}
}
