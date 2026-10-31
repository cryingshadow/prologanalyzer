package prologanalyzer.structure;

import prologanalyzer.*;
import prologparser.*;

public record GlobalState(PrologProgram program, FreshNameGenerator fridge, ScopeGenerator scoper) {

}
