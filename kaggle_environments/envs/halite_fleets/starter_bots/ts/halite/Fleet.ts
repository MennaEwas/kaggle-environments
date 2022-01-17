import {Board} from "./Board";
import {Cell} from "./Cell";
import {Direction} from "./Direction";
import {Player} from "./Player";
import {Point} from "./Point";

public class Fleet {

    public readonly id: string;
    public shipCount: number;
    public direction: Direction;
    public position: Point;
    public flightPlan: string;
    public halite: number;
    public readonly playerId: number;
    public readonly board: Object;
    
    public constructor(fleetId: string, shipCount: number, direction: Direction, position: Point, halite: number, flightPlan: string, playerId: number, board: Board) {
        this.id = fleetId;
        this.shipCount = shipCount;
        this.direction = direction;
        this.position = position;
        this.flightPlan = flightPlan;
        this.halite = halite;
        this.playerId = playerId;
        this.board = board;
    }

    public cloneToBoard(board: Board): Fleet {
        return new Fleet(this.id, this.shipCount, this.direction, this.position, this.halite, this.flightPlan, this.playerId, board);
    }

    public cell(): Cell {
        return this.board.getCellAtPosition(this.position);
    }

    public player(): Player {
        return this.board.players[this.playerId];
    }

    public collectionRate(): number {
        return Math.min(Math.log(this.shipCount) / 10, .99);
    }

    /**
     * Returns the length of the longest possible flight plan this fleet can be assigned
     * @return
     */
    public static maxFlightPlanLenForShipCount(shipCount: number): number {
        return (Math.floor(2 * Math.log(shipCount)) + 1);
    }

    /**
     * Converts a fleet back to the normalized observation subset that constructed it.
     */
    public observation(): string[] {
        return [
            this.position.toIndex(this.board.configuration.size).toString(), 
            this.halite.toString(),
            this.shipCount.toString(),
            this.direction.toIndex().toString(),
            this.flightPlan
        ];
    }

    public lessThanOtherAlliedFleet(other: Fleet): boolean {
        if (this.shipCount != other.shipCount) {
            return this.shipCount < other.shipCount;
        }
        if (this.halite != other.halite) {
            return this.halite < other.halite;
        }
        return this.direction.toIndex() > other.direction.toIndex();
}

    }
