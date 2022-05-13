/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package software.amazon.smithy.rust.codegen.server.smithy

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import software.amazon.smithy.model.shapes.ListShape
import software.amazon.smithy.model.shapes.ServiceShape
import software.amazon.smithy.model.shapes.StructureShape
import software.amazon.smithy.rust.codegen.rustlang.RustType
import software.amazon.smithy.rust.codegen.rustlang.render
import software.amazon.smithy.rust.codegen.server.smithy.testutil.serverTestSymbolProvider
import software.amazon.smithy.rust.codegen.smithy.UnconstrainedShapeSymbolProvider
import software.amazon.smithy.rust.codegen.smithy.rustType
import software.amazon.smithy.rust.codegen.testutil.asSmithyModel
import software.amazon.smithy.rust.codegen.util.lookup

// TODO I can't move this file to `codegen` subproject because `serverTestSymbolProvider` is only in the `codegen-server`
//  subproject, but I need `UnconstrainedSymbolProvider` to be in the `codegen` subproject because it's used in the
//  `JsonParserGenerator`.
class UnconstrainedShapeSymbolProviderTest {
    private val baseModelString =
        """
        namespace test

        service TestService {
            version: "123",
            operations: [TestOperation]
        }
        
        operation TestOperation {
            input: TestInputOutput,
            output: TestInputOutput,
        }
        
        structure TestInputOutput {
            list: ListA
        }
        """

    @Test
    fun `it should adjust types for unconstrained shapes`() {
        val model =
            """
            $baseModelString
            
            list ListA {
                member: ListB
            }
            
            list ListB {
                member: StructureC
            }
            
            structure StructureC {
                @required
                string: String
            }
            """.asSmithyModel()

        val serviceShape = model.lookup<ServiceShape>("test#TestService")
        val symbolProvider = UnconstrainedShapeSymbolProvider(serverTestSymbolProvider(model, serviceShape), model, serviceShape)

        val listAShape = model.lookup<ListShape>("test#ListA")
        val listAType = symbolProvider.toSymbol(listAShape).rustType()

        val listBShape = model.lookup<ListShape>("test#ListB")
        val listBType = symbolProvider.toSymbol(listBShape).rustType()

        val structureCShape = model.lookup<StructureShape>("test#StructureC")
        val structureCType = symbolProvider.toSymbol(structureCShape).rustType()

        listAType shouldBe RustType.Opaque("ListAUnconstrained", "crate::unconstrained::list_a_unconstrained")
        listBType shouldBe RustType.Opaque("ListBUnconstrained", "crate::unconstrained::list_b_unconstrained")
        structureCType shouldBe RustType.Opaque("Builder", "crate::model::structure_c")
    }

    @Test
    fun `it should delegate to the base symbol provider if called with a shape that cannot reach a constrained shape`() {
        val model =
            """
            $baseModelString
            
            list ListA {
                member: StructureB
            }
            
            structure StructureB {
                string: String
            }
            """.asSmithyModel()

        val serviceShape = model.lookup<ServiceShape>("test#TestService")
        val symbolProvider = UnconstrainedShapeSymbolProvider(serverTestSymbolProvider(model, serviceShape), model, serviceShape)

        val listAShape = model.lookup<ListShape>("test#ListA")
        val structureBShape = model.lookup<StructureShape>("test#StructureB")

        symbolProvider.toSymbol(structureBShape).rustType().render() shouldBe "crate::model::StructureB"
        symbolProvider.toSymbol(listAShape).rustType().render() shouldBe "std::vec::Vec<crate::model::StructureB>"
    }
}