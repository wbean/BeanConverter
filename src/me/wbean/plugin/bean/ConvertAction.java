package me.wbean.plugin.bean;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.util.TreeFileChooser;
import com.intellij.ide.util.TreeFileChooserFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;

/**
 * Copyright ©2014-2019 Youzan.com All rights reserved
 * PACKAGE_NAME
 */
public class ConvertAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        VirtualFile source = e.getData(PlatformDataKeys.VIRTUAL_FILE);
        if(null == source){
            return;
        }

        PsiJavaFile sourcePsiFile = (PsiJavaFile) PsiManager.getInstance(e.getProject()).findFile(source);
        PsiClass sourcePsiClass = sourcePsiFile.getClasses()[0];

        TreeFileChooser treeFileChooser = TreeFileChooserFactory.getInstance(e.getProject()).createFileChooser("choose target bean", null, JavaFileType.INSTANCE, null, true, false);
        treeFileChooser.showDialog();
        PsiFile target = treeFileChooser.getSelectedFile();
        if(null == target){
            return;
        }

        PsiJavaFile targetPsiFile = (PsiJavaFile) target;
        PsiClass targetPsiClass = targetPsiFile.getClasses()[0];


        Map<String, PsiMethod> sourceMap = getMap(sourcePsiClass, "get");
        Map<String, PsiMethod> targetMap = getMap(targetPsiClass, "set");

        
        StringBuilder finalCode = new StringBuilder();

        finalCode.append(String.format("public static %s convert(%s source){\n", targetPsiClass.getName(), sourcePsiClass.getName()));
        
        finalCode.append("    if (null == source) {\n");
        finalCode.append("        return null;\n");
        finalCode.append("    }\n");

        finalCode.append("\n");

        finalCode.append(String.format("    %s target = new %s();\n", targetPsiClass.getName(), targetPsiClass.getName()));
        for (Map.Entry<String, PsiMethod> targetPsiMethodEntry : targetMap.entrySet()) {
            TreeMap<Integer, String> bestMatch = new TreeMap<>();
            String targetKey = targetPsiMethodEntry.getKey();

            for (Map.Entry<String, PsiMethod> sourcePsiMethodEntry : sourceMap.entrySet()) {
                String sourceKey = sourcePsiMethodEntry.getKey();
                int diff = StringUtils.getLevenshteinDistance(targetKey, sourceKey);
                bestMatch.put(diff, sourceKey);
            }

            String bestKey = bestMatch.pollFirstEntry().getValue();
            finalCode.append(String.format("    target.%s(source.%s());\n",targetPsiMethodEntry.getValue().getName(),sourceMap.get(bestKey).getName()));
        }

        finalCode.append("    return target;\n");
        finalCode.append("}");

        Messages.showMessageDialog(finalCode.toString(),"Ctrl-V paste this code wherever you want", null);

        StringSelection selection = new StringSelection(finalCode.toString());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    private Map<String, PsiMethod> getMap(PsiClass psiClass, String methodPerfix) {
        PsiMethod[] sourcePsiClassAllMethods = psiClass.getAllMethods();

        Map<String, PsiMethod> sourceMap = new HashMap<>();
        for (PsiMethod sourcePsiClassAllMethod : sourcePsiClassAllMethods) {
            if(sourcePsiClassAllMethod.getName().startsWith(methodPerfix)){
                sourceMap.put(getAttrName(sourcePsiClassAllMethod.getName()), sourcePsiClassAllMethod);
            }
        }
        return sourceMap;
    }

    private String getAttrName(String methodName){
        return methodName.substring(3).toLowerCase();
    }
}
