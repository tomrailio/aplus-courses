package fi.aalto.cs.apluscourses.intellij.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.jetbrains.plugins.scala.lang.psi.api.base.ScPatternList;

public class ScalaVariableDeclaration {

  private final String variableType;
  private final String variableName;
  private final String[] valueTokens;

  /**
   * Constructor.
   */
  public ScalaVariableDeclaration(String variableType, String variableName, String[] valueTokens) {
    this.variableType = variableType;
    this.variableName = variableName;
    this.valueTokens = valueTokens;
  }

  /**
   * Checks the name of the declared variable.
   */
  public boolean checkVariableName(Optional<PsiElement> refPattern) {
    return refPattern.isPresent() && variableName.equals(refPattern.get().getText());
  }

  /**
   * Check if the variable is of the specified var or val type.
   */
  public boolean checkVariableType(Optional<PsiElement> element) {
    if (element.isPresent()) {
      ScPatternList patternList = (ScPatternList) element.get();
      Collection<PsiElement> allChildren = PsiUtil.getPsiElementsSiblings(patternList);
      return allChildren.stream().anyMatch(elem -> elem instanceof LeafPsiElement
          && elem.getText().equals(variableType));
    }
    return false;
  }

  /**
   * Checks the tokens of the assigned value.
   */
  public boolean checkAssignedValue(Optional<PsiElement> refPattern) {
    if (refPattern.isPresent()) {
      Collection<PsiElement> siblings = PsiUtil.getPsiElementsSiblings(refPattern.get());
      Optional<PsiElement> equalsOptional = siblings.stream().filter(s -> s.getText().equals("=")).findFirst();
      if (equalsOptional.isPresent()) {
        PsiElement equals = equalsOptional.get();
        List<String> fileValueTokens = new ArrayList<>();
        while (equals.getNextSibling() != null) {
          equals = equals.getNextSibling();
          if (!equals.getText().trim().equals("")) {
            fileValueTokens.add(equals.getText());
          }
        }
        return Arrays.equals(fileValueTokens.toArray(), valueTokens);
      }
    }
    return false;
  }
}
