package de.erethon.factions.war.entities.caravans;

public final class ActiveCaravanRoute {
    private final CaravanRoute route;
    private int currentNodeIndex;
    private final int supplies;

    public ActiveCaravanRoute(CaravanRoute route, CaravanRouteNode currentNode, int supplies) {
        this.route = route;
        this.supplies = supplies;
    }

    public CaravanRoute route() {
        return route;
    }

    public CaravanRouteNode currentNode() {
        return route.nodes()[currentNodeIndex];
    }

    public CaravanRouteNode nextNode() {
        if (isAtEnd()) {
            return currentNode();
        }
        return route.nodes()[currentNodeIndex + 1];
    }

    public int currentNodeIndex() {
        return currentNodeIndex;
    }

    public void advance() {
        if (!isAtEnd()) {
            currentNodeIndex++;
        }
    }

    public boolean isAtEnd() {
        return currentNodeIndex == route.nodes().length - 1;
    }

    public int supplies() {
        return supplies;
    }

    @Override
    public String toString() {
        return "ActiveCaravanRoute[" +
                "route=" + route + ", " +
                "currentNode=" + currentNodeIndex + ", " +
                "supplies=" + supplies + ']';
    }

}
