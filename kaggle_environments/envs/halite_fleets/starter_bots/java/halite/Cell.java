package halite;

import java.util.Optional;

public class Cell {

    public final Point position;
    public final int halite;
    public final String shipyardId;
    public final String fleetId;
    public final Board board;
    
    public Cell(Point position, int halite, String shipyardId, String fleetId, Board board) {
        this.position = position;
        this.halite = halite;
        this.shipyardId = shipyardId;
        this.fleetId = fleetId;
        this.board = board;
    }


    public Optional<Fleet> fleet() {
        if (this.board.fleets.containsKey(this.fleetId)) {
            return Optional.of(this.board.fleets.get(this.fleetId));
        }
        return Optional.empty();
    }


    public Optional<Shipyard> shipyard() {
        if (this.board.shipyards.containsKey(this.shipyardId)) {
            return Optional.of(this.board.shipyards.get(this.shipyardId));
        }
        return Optional.empty();
    }

    public Cell neighbor(Point offset) {
        Point next = this.position.translate(offset, this.board.size());
        return this.board.getCellAtPosition(next);
    }


    public Cell north() {
        return this.neighbor(Direction.NORTH);
    }

    public Cell south() {
        return this.neighbor(((Point)Direction.SOUTH));
    }

    public Cell east() {
        return this.neighbor(Direction.EAST);
    }

    public Cell west() {
        return this.neighbor(Direction.WEST);
    }
}
