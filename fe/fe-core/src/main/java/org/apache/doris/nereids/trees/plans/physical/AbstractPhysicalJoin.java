// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.doris.nereids.trees.plans.physical;

import org.apache.doris.nereids.memo.GroupExpression;
import org.apache.doris.nereids.properties.LogicalProperties;
import org.apache.doris.nereids.properties.PhysicalProperties;
import org.apache.doris.nereids.trees.expressions.Expression;
import org.apache.doris.nereids.trees.plans.JoinType;
import org.apache.doris.nereids.trees.plans.Plan;
import org.apache.doris.nereids.trees.plans.PlanType;
import org.apache.doris.nereids.trees.plans.algebra.Join;
import org.apache.doris.nereids.util.ExpressionUtils;

import com.google.common.collect.ImmutableList.Builder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Abstract class for all physical join node.
 */
public abstract class AbstractPhysicalJoin<
        LEFT_CHILD_TYPE extends Plan,
        RIGHT_CHILD_TYPE extends Plan>
        extends PhysicalBinary<LEFT_CHILD_TYPE, RIGHT_CHILD_TYPE> implements Join {
    protected final JoinType joinType;

    protected final List<Expression> hashJoinConjuncts;

    protected final Optional<Expression> otherJoinCondition;

    /**
     * Constructor of PhysicalJoin.
     *
     * @param joinType Which join type, left semi join, inner join...
     * @param condition join condition.
     */
    public AbstractPhysicalJoin(PlanType type, JoinType joinType, List<Expression> hashJoinConjuncts,
            Optional<Expression> condition,
            Optional<GroupExpression> groupExpression, LogicalProperties logicalProperties,
            LEFT_CHILD_TYPE leftChild, RIGHT_CHILD_TYPE rightChild) {
        super(type, groupExpression, logicalProperties, leftChild, rightChild);
        this.joinType = Objects.requireNonNull(joinType, "joinType can not be null");
        this.hashJoinConjuncts = hashJoinConjuncts;
        this.otherJoinCondition = Objects.requireNonNull(condition, "condition can not be null");
    }

    /**
     * Constructor of PhysicalJoin.
     *
     * @param joinType Which join type, left semi join, inner join...
     * @param condition join condition.
     */
    public AbstractPhysicalJoin(PlanType type, JoinType joinType, List<Expression> hashJoinConjuncts,
            Optional<Expression> condition, Optional<GroupExpression> groupExpression,
            LogicalProperties logicalProperties, PhysicalProperties physicalProperties,
            LEFT_CHILD_TYPE leftChild, RIGHT_CHILD_TYPE rightChild) {
        super(type, groupExpression, logicalProperties, physicalProperties, leftChild, rightChild);
        this.joinType = Objects.requireNonNull(joinType, "joinType can not be null");
        this.hashJoinConjuncts = hashJoinConjuncts;
        this.otherJoinCondition = Objects.requireNonNull(condition, "condition can not be null");
    }

    public List<Expression> getHashJoinConjuncts() {
        return hashJoinConjuncts;
    }

    public JoinType getJoinType() {
        return joinType;
    }

    public Optional<Expression> getOtherJoinCondition() {
        return otherJoinCondition;
    }

    @Override
    public List<? extends Expression> getExpressions() {
        Builder<Expression> builder = new Builder<Expression>()
                .addAll(hashJoinConjuncts);
        otherJoinCondition.ifPresent(builder::add);
        return builder.build();
    }

    // TODO:
    // 1. consider the order of conjucts in otherJoinCondition and hashJoinConditions
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        AbstractPhysicalJoin<?, ?> that = (AbstractPhysicalJoin<?, ?>) o;
        return joinType == that.joinType
                && hashJoinConjuncts.equals(that.hashJoinConjuncts)
                && otherJoinCondition.equals(that.otherJoinCondition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), joinType, hashJoinConjuncts, otherJoinCondition);
    }

    /**
     * hashJoinConjuncts and otherJoinCondition
     *
     * @return the combination of hashJoinConjuncts and otherJoinCondition
     */
    public Optional<Expression> getOnClauseCondition() {
        Optional<Expression> hashJoinCondition = ExpressionUtils.optionalAnd(hashJoinConjuncts);

        if (hashJoinCondition.isPresent() && otherJoinCondition.isPresent()) {
            return ExpressionUtils.optionalAnd(hashJoinCondition.get(), otherJoinCondition.get());
        }

        return hashJoinCondition.map(Optional::of)
                .orElse(otherJoinCondition);
    }
}
