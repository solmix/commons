/*
 * Copyright 2014 The Solmix Project
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.gnu.org/licenses/ 
 * or see the FSF site: http://www.fsf.org. 
 */
package org.solmix.commons.expr.composite;

import java.util.LinkedList;
import java.util.List;

import org.solmix.commons.expr.Expression;
import org.solmix.commons.expr.ExpressionFactory;
import org.solmix.commons.expr.ExpressionParseException;
import org.solmix.commons.expr.jexl.JexlExpressionFactory;


/**
 * 
 * @author solmix.f@gmail.com
 * @version $Id$  2015年6月12日
 */

public class CompositeExpressionFactory implements ExpressionFactory
{
    private ExpressionFactory factory;

    /** 创建一个组合表达式的工厂，默认使用<code>JexlExpressionFactory</code>来解析子表达式。 */
    public CompositeExpressionFactory() {
        this.factory = new JexlExpressionFactory();
    }

    /**
     * 创建一个组合表达式的工厂。
     *
     * @param factory 创建组合表达式中的子表达式的工厂
     */
    public CompositeExpressionFactory(ExpressionFactory factory) {
        this.factory = factory;
    }

    /**
     * 创建表达式。
     * <ul>
     * <li>如果表达式中不包含“<code>${...}</code>”，则创建<code>ConstantExpression</code>。</li>
     * <li>如果表达式以“<code>${</code>”开始，并以“<code>}</code>”结尾，则调用指定的
     * <code>ExpressionFactory</code>来创建非组合表达式。</li>
     * <li>如果表达式包含“<code>${...}</code>”，但在此之外还有别的字符，则创建
     * <code>CompositeExpression</code>。</li>
     * </ul>
     *
     * @param expr 表达式字符串
     * @return 表达式
     */
    public Expression createExpression(String expr) throws ExpressionParseException {
        int length = expr.length();
        int startIndex = expr.indexOf("${");

        // 如果表达式不包含${}，则创建constant expression。
        if (startIndex < 0) {
            return new ConstantExpression(expr);
        }

        int endIndex = expr.indexOf("}", startIndex + 2);

        if (endIndex < 0) {
            throw new ExpressionParseException("Missing '}' character at the end of expression: " + expr);
        }

        // 如果表达式以${开头，以}结尾，则直接调用factory来创建表达式。
        if (startIndex == 0 && endIndex == length - 1) {
            return factory.createExpression(expr.substring(2, endIndex));
        }

        // 创建复合的表达式。
        List<Expression> expressions = new LinkedList<Expression>();
        char ch = 0;
        int i = 0;

        StringBuffer chars = new StringBuffer();
        StringBuffer exprBuff = new StringBuffer();

        MAIN:
        while (i < length) {
            ch = expr.charAt(i);

            switch (ch) {
                case '$': {
                    if (i + 1 < length) {
                        ++i;
                        ch = expr.charAt(i);

                        switch (ch) {
                            case '$': {
                                chars.append(ch);
                                break;
                            }

                            case '{': {
                                if (chars.length() > 0) {
                                    expressions.add(new ConstantExpression(chars.toString()));
                                    chars.delete(0, chars.length());
                                }

                                if (i + 1 < length) {
                                    ++i;

                                    while (i < length) {
                                        ch = expr.charAt(i);

                                        {
                                            switch (ch) {
                                                case '"': {
                                                    exprBuff.append(ch);
                                                    ++i;

                                                    DOUBLE_QUOTE:
                                                    while (i < length) {
                                                        ch = expr.charAt(i);

                                                        switch (ch) {
                                                            case '\\': {
                                                                ++i;
                                                                exprBuff.append(ch);
                                                                break;
                                                            }

                                                            case '"': {
                                                                ++i;
                                                                exprBuff.append(ch);
                                                                break DOUBLE_QUOTE;
                                                            }

                                                            default: {
                                                                ++i;
                                                                exprBuff.append(ch);
                                                            }
                                                        }
                                                    }

                                                    break;
                                                }

                                                case '\'': {
                                                    exprBuff.append(ch);
                                                    ++i;

                                                    SINGLE_QUOTE:
                                                    while (i < length) {
                                                        ch = expr.charAt(i);

                                                        switch (ch) {
                                                            case '\\': {
                                                                ++i;
                                                                exprBuff.append(ch);
                                                                break;
                                                            }

                                                            case '\'': {
                                                                ++i;
                                                                exprBuff.append(ch);
                                                                break SINGLE_QUOTE;
                                                            }

                                                            default: {
                                                                ++i;
                                                                exprBuff.append(ch);
                                                            }
                                                        }
                                                    }

                                                    break;
                                                }

                                                case '}': {
                                                    expressions.add(factory.createExpression(exprBuff.toString()));

                                                    exprBuff.delete(0, exprBuff.length());
                                                    ++i;
                                                    continue MAIN;
                                                }

                                                default: {
                                                    exprBuff.append(ch);
                                                    ++i;
                                                }
                                            }
                                        }
                                    }
                                }

                                break;
                            }

                            default:
                                chars.append(ch);
                        }
                    } else {
                        chars.append(ch);
                    }

                    break;
                }

                default:
                    chars.append(ch);
            }

            ++i;
        }

        if (chars.length() > 0) {
            expressions.add(new ConstantExpression(chars.toString()));
        }

        return new CompositeExpression(expr, expressions);
    }
}