package de.erethon.factions.war.entities.caravans;

import java.util.Objects;

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

    public int currentNodeIndex() {
        return currentNodeIndex;
    }

    public void advance() {
        currentNodeIndex++;
    }

    public boolean isAtEnd() {
        return currentNodeIndex == route.nodes().length - 1;
    }

    public int supplies() {
        return supplies;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ActiveCaravanRoute) obj;
        return Objects.equals(this.route, that.route) &&
                currentNodeIndex == that.currentNodeIndex &&
                this.supplies == that.supplies;
    }

    @Override
    public int hashCode() {
        return Objects.hash(route, currentNodeIndex, supplies);
    }

    @Override
    public String toString() {
        return "ActiveCaravanRoute[" +
                "route=" + route + ", " +
                "currentNode=" + currentNodeIndex + ", " +
                "supplies=" + supplies + ']';
    }

}
